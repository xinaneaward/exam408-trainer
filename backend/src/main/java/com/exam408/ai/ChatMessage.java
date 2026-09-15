package com.exam408.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 发送给大模型的消息（OpenAI兼容格式） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /** system / user / assistant */
    private String role;

    private String content;
}
