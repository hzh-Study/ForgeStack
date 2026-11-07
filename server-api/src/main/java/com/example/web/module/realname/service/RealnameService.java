package com.example.web.module.realname.service;

import com.example.web.common.page.PageResult;
import com.example.web.module.realname.dto.RealnameQueryDTO;
import com.example.web.module.realname.dto.RealnameSubmitDTO;
import com.example.web.module.realname.vo.RealnameDetailVO;
import com.example.web.module.realname.vo.RealnameListVO;

/**
 * 实名认证业务接口。
 */
public interface RealnameService {

    /**
     * 提交当前账号的个人实名信息并同步返回校验结果。
     *
     * @param dto 实名提交参数
     * @return 实名详情
     */
    RealnameDetailVO submitPersonal(RealnameSubmitDTO dto);

    /**
     * 查询当前账号的个人实名信息，本人可见完整证件号。
     *
     * @return 实名详情，未提交时返回 null
     */
    RealnameDetailVO getCurrentRealname();

    /**
     * 管理员分页查询实名记录，列表证件号脱敏。
     *
     * @param dto 查询参数
     * @return 实名分页数据
     */
    PageResult<RealnameListVO> pageRealnames(RealnameQueryDTO dto);

    /**
     * 管理员查看单条实名详情，可见完整证件号。
     *
     * @param id 实名记录 ID
     * @return 实名详情
     */
    RealnameDetailVO getAdminRealnameDetail(Long id);
}
