import { useState, useCallback, useRef } from "react";

import { NotifyProps } from "@/components/Notify";

type ClosedNotification = Pick<NotifyProps, "kind" | "visible" | "onClose" | "text">

type Props = {
    success: Omit<NotifyProps, "onClose" | "kind" | "visible">;
    failed: Omit<NotifyProps, "onClose" | "kind" | "visible">;
}
export const useNotification = ({ failed, success }: Props) => {
    const [notificationType, setNotificationType] = useState<"Success" | "Failed" | "">("");
    const dynamicText = useRef<string | null>(null);

    const clear = useCallback(() => {
        setNotificationType("");
    }, []);

    let notifyProps: Omit<NotifyProps, "onClose"> | ClosedNotification = notificationType === "Success" ? {
        kind: "success",
        visible: true,
        ...success,
        text: dynamicText.current || success.text
    } : notificationType === "Failed" ? {
        kind: "fail",
        visible: true,
        ...failed,
        text: dynamicText.current || failed.text
    } : { visible: false, kind: "fail", onClose: () => {}, text: "" };

    const setNotification = (type: "Success" | "Failed", text?: string) => {
        setNotificationType(type);
        text ? (dynamicText.current = text) : (dynamicText.current = null);
    }

    return [setNotification, { props: notifyProps, clear }] as const;
};