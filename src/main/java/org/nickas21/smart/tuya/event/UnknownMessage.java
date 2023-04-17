package org.nickas21.smart.tuya.event;

import com.alibaba.fastjson.JSONObject;

/**
 * Description: TODO
 *
 * @author Chyern
 * @since 2021/9/17
 */
public class UnknownMessage extends BaseTuyaMessage {

    private JSONObject messageBody;

    @Override
    public void defaultBuild(SourceMessage sourceMessage, JSONObject messageBody) {
        super.defaultBuild(sourceMessage, messageBody);
        this.messageBody = messageBody;
    }

    @Override
    public EventType getEventType() {
        return EventType.UNKNOWN_MESSAGE;
    }

}
