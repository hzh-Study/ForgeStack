package com.example.web.module.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.web.exception.BusinessException;
import com.example.web.exception.ForbiddenException;
import com.example.web.module.account.entity.Account;
import com.example.web.module.account.enums.AccountRoleEnum;
import com.example.web.module.checkin.entity.CheckinPhoto;
import com.example.web.module.checkin.mapper.CheckinPhotoMapper;
import com.example.web.module.file.entity.StoredFile;
import com.example.web.module.file.mapper.StoredFileMapper;
import com.example.web.module.onboarding.entity.Onboarding;
import com.example.web.module.onboarding.entity.OnboardingFile;
import com.example.web.module.onboarding.mapper.OnboardingFileMapper;
import com.example.web.module.onboarding.mapper.OnboardingMapper;
import com.example.web.module.order.entity.Order;
import com.example.web.module.order.mapper.OrderMapper;
import com.example.web.module.reimbursement.entity.Reimbursement;
import com.example.web.module.reimbursement.entity.ReimbursementPhoto;
import com.example.web.module.reimbursement.mapper.ReimbursementMapper;
import com.example.web.module.reimbursement.mapper.ReimbursementPhotoMapper;
import com.example.web.module.task.entity.Task;
import com.example.web.module.task.mapper.TaskMapper;
import com.example.web.security.CurrentAccountService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 文件读取鉴权服务，按上传者、业务参与方与管理员身份校验访问范围。
 */
@Service
public class FileAccessService {

    private final StoredFileMapper storedFileMapper;
    private final CheckinPhotoMapper checkinPhotoMapper;
    private final TaskMapper taskMapper;
    private final OrderMapper orderMapper;
    private final ReimbursementPhotoMapper reimbursementPhotoMapper;
    private final ReimbursementMapper reimbursementMapper;
    private final OnboardingFileMapper onboardingFileMapper;
    private final OnboardingMapper onboardingMapper;
    private final CurrentAccountService currentAccountService;

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    @Value("${app.file.access-prefix}")
    private String accessPrefix;

    public FileAccessService(StoredFileMapper storedFileMapper, CheckinPhotoMapper checkinPhotoMapper,
                             TaskMapper taskMapper, OrderMapper orderMapper,
                             ReimbursementPhotoMapper reimbursementPhotoMapper,
                             ReimbursementMapper reimbursementMapper,
                             OnboardingFileMapper onboardingFileMapper, OnboardingMapper onboardingMapper,
                             CurrentAccountService currentAccountService) {
        this.storedFileMapper = storedFileMapper;
        this.checkinPhotoMapper = checkinPhotoMapper;
        this.taskMapper = taskMapper;
        this.orderMapper = orderMapper;
        this.reimbursementPhotoMapper = reimbursementPhotoMapper;
        this.reimbursementMapper = reimbursementMapper;
        this.onboardingFileMapper = onboardingFileMapper;
        this.onboardingMapper = onboardingMapper;
        this.currentAccountService = currentAccountService;
    }

    /**
     * 校验当前登录账号是否可以读取指定文件。
     *
     * @param filePath 文件相对路径
     * @return 可读取的本地文件路径
     */
    public Path resolveReadable(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new BusinessException("文件路径不能为空");
        }
        Account account = currentAccountService.getAccount();
        StoredFile storedFile = storedFileMapper.selectOne(new LambdaQueryWrapper<StoredFile>()
                .eq(StoredFile::getFilePath, filePath));
        if (storedFile == null) {
            throw new BusinessException("文件不存在");
        }
        if (!canRead(storedFile, account)) {
            throw new ForbiddenException("无权访问该文件");
        }

        String relative = filePath;
        if (relative.startsWith(accessPrefix)) {
            relative = relative.substring(accessPrefix.length());
        }
        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path target = base.resolve(relative.replaceFirst("^/", "")).normalize();
        if (!target.startsWith(base) || !Files.exists(target) || !Files.isRegularFile(target)) {
            throw new BusinessException("文件不存在");
        }
        return target;
    }

    /**
     * 按文件扩展名推断响应内容类型。
     *
     * @param path 文件路径
     * @return 内容类型
     */
    public String getContentType(Path path) {
        try {
            String probed = Files.probeContentType(path);
            if (probed != null) {
                return probed;
            }
        } catch (IOException ignored) {
            // 探测失败时按扩展名兜底
        }
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (name.endsWith(".gif")) {
            return "image/gif";
        }
        if (name.endsWith(".webp")) {
            return "image/webp";
        }
        if (name.endsWith(".pdf")) {
            return "application/pdf";
        }
        return "application/octet-stream";
    }

    /**
     * 判断账号是否属于该文件的业务参与方。
     *
     * @param storedFile 文件记录
     * @param account 当前账号
     * @return true 表示可读取
     */
    private boolean canRead(StoredFile storedFile, Account account) {
        if (account.getRole() == AccountRoleEnum.ADMIN) {
            return true;
        }
        if (account.getId().equals(storedFile.getUploaderAccountId())) {
            return true;
        }

        List<CheckinPhoto> checkinPhotos = checkinPhotoMapper.selectList(new LambdaQueryWrapper<CheckinPhoto>()
                .eq(CheckinPhoto::getFilePath, storedFile.getFilePath()));
        if (!checkinPhotos.isEmpty()) {
            Task task = taskMapper.selectById(checkinPhotos.get(0).getTaskId());
            if (task == null) {
                return false;
            }
            if (account.getId().equals(task.getDriverAccountId())
                    || account.getId().equals(task.getLogisticsAccountId())) {
                return true;
            }
            Order order = orderMapper.selectById(task.getOrderId());
            return order != null && account.getId().equals(order.getShipperAccountId());
        }

        List<ReimbursementPhoto> reimbursementPhotos = reimbursementPhotoMapper.selectList(
                new LambdaQueryWrapper<ReimbursementPhoto>().eq(ReimbursementPhoto::getFilePath, storedFile.getFilePath()));
        if (!reimbursementPhotos.isEmpty()) {
            Reimbursement reimbursement = reimbursementMapper.selectById(reimbursementPhotos.get(0).getReimbursementId());
            return reimbursement != null && (account.getId().equals(reimbursement.getDriverAccountId())
                    || account.getId().equals(reimbursement.getLogisticsAccountId()));
        }

        List<OnboardingFile> onboardingFiles = onboardingFileMapper.selectList(new LambdaQueryWrapper<OnboardingFile>()
                .eq(OnboardingFile::getFilePath, storedFile.getFilePath()));
        if (!onboardingFiles.isEmpty()) {
            Onboarding onboarding = onboardingMapper.selectById(onboardingFiles.get(0).getOnboardingId());
            return onboarding != null && account.getId().equals(onboarding.getAccountId());
        }
        return false;
    }
}
