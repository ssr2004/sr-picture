package com.yupi.yupicturebackend.manager.websocket.disrupter;

import com.yupi.yupicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.yupi.yupicturebackend.model.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * 上下文容器
 */
@Data
public class PictureEditEvent {

    /**
     * 消息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 当前用户的 session
     */
    private WebSocketSession session;
    
    /**
     * 当前用户
     */
    private User user;

    /**
     * 图片 id
     */
    private Long pictureId;

}