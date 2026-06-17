package com.coldchain.vaccine.dto;

import lombok.Data;

@Data
public class WechatConfigDTO {

    private String corpId;

    private String agentId;

    private String appSecret;

    private String toUser;

    private String toParty;

    private String toTag;
}
