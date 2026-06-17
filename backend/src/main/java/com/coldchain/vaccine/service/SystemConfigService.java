package com.coldchain.vaccine.service;

import com.coldchain.vaccine.dto.WechatConfigDTO;
import com.coldchain.vaccine.entity.SystemConfig;
import com.coldchain.vaccine.repository.SystemConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class SystemConfigService {

    private static final Logger logger = LoggerFactory.getLogger(SystemConfigService.class);

    private static final String KEY_WECHAT_CORP_ID = "wechat.corp.id";
    private static final String KEY_WECHAT_AGENT_ID = "wechat.agent.id";
    private static final String KEY_WECHAT_APP_SECRET = "wechat.app.secret";
    private static final String KEY_WECHAT_TO_USER = "wechat.to.user";
    private static final String KEY_WECHAT_TO_PARTY = "wechat.to.party";
    private static final String KEY_WECHAT_TO_TAG = "wechat.to.tag";

    private final SystemConfigRepository configRepository;

    public SystemConfigService(SystemConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Transactional(readOnly = true)
    public WechatConfigDTO getWechatConfig() {
        WechatConfigDTO dto = new WechatConfigDTO();
        dto.setCorpId(getConfigValue(KEY_WECHAT_CORP_ID));
        dto.setAgentId(getConfigValue(KEY_WECHAT_AGENT_ID));
        dto.setAppSecret(getConfigValue(KEY_WECHAT_APP_SECRET));
        dto.setToUser(getConfigValue(KEY_WECHAT_TO_USER));
        dto.setToParty(getConfigValue(KEY_WECHAT_TO_PARTY));
        dto.setToTag(getConfigValue(KEY_WECHAT_TO_TAG));
        return dto;
    }

    @Transactional
    public void saveWechatConfig(WechatConfigDTO dto) {
        saveConfig(KEY_WECHAT_CORP_ID, dto.getCorpId(), "微信企业号 CorpId");
        saveConfig(KEY_WECHAT_AGENT_ID, dto.getAgentId(), "微信企业号 AgentId");
        saveConfig(KEY_WECHAT_APP_SECRET, dto.getAppSecret(), "微信企业号 AppSecret");
        saveConfig(KEY_WECHAT_TO_USER, dto.getToUser(), "接收人 UserId（多个用 | 分隔，@all 表示全部）");
        saveConfig(KEY_WECHAT_TO_PARTY, dto.getToParty(), "接收部门 ID（多个用 | 分隔）");
        saveConfig(KEY_WECHAT_TO_TAG, dto.getToTag(), "接收标签 ID（多个用 | 分隔）");
        logger.info("微信配置已更新");
    }

    @Transactional(readOnly = true)
    public Map<String, String> getAllConfigs() {
        Map<String, String> result = new HashMap<>();
        for (SystemConfig config : configRepository.findAll()) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public boolean isWechatConfigured() {
        String corpId = getConfigValue(KEY_WECHAT_CORP_ID);
        String agentId = getConfigValue(KEY_WECHAT_AGENT_ID);
        String secret = getConfigValue(KEY_WECHAT_APP_SECRET);
        boolean hasReceiver = !isEmpty(getConfigValue(KEY_WECHAT_TO_USER))
                || !isEmpty(getConfigValue(KEY_WECHAT_TO_PARTY))
                || !isEmpty(getConfigValue(KEY_WECHAT_TO_TAG));
        return !isEmpty(corpId) && !isEmpty(agentId) && !isEmpty(secret) && hasReceiver;
    }

    private String getConfigValue(String key) {
        return configRepository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(null);
    }

    private void saveConfig(String key, String value, String description) {
        Optional<SystemConfig> opt = configRepository.findByConfigKey(key);
        SystemConfig config;
        if (opt.isPresent()) {
            config = opt.get();
        } else {
            config = new SystemConfig();
            config.setConfigKey(key);
        }
        config.setConfigValue(value);
        config.setDescription(description);
        configRepository.save(config);
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
