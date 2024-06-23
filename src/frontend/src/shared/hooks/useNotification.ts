import { useState, useCallback, useRef } from "react";

import { NotifyProps } from "@/components/Notify";

const extraDelay = 50;

type ClosedNotification = Pick<
  NotifyProps,
  "kind" | "visible" | "onClose" | "text"
>;

type Props = {
  success: Omit<NotifyProps, "onClose" | "kind" | "visible">;
  failed: Omit<NotifyProps, "onClose" | "kind" | "visible">;
};
export const useNotification = ({ failed, success }: Props) => {
  const [loading, setLoading] = useState(0);
  const [notificationType, setNotificationType] = useState<
    "Success" | "Failed" | ""
  >("");
  const dynamicText = useRef<string | null>(null);

  const clear = useCallback(() => {
    setNotificationType("");
    setLoading(0);
  }, []);

  let notifyProps: Omit<NotifyProps, "onClose"> | ClosedNotification =
    notificationType === "Success"
      ? {
          kind: "success",
          visible: true,
          ...success,
          text: dynamicText.current || success.text,
        }
      : notificationType === "Failed"
      ? {
          kind: "fail",
          visible: true,
          ...failed,
          text: dynamicText.current || failed.text,
        }
      : { visible: false, kind: "fail", onClose: () => {}, text: "" };

  const setNotification = (type: "Success" | "Failed", text?: string) => {
    const op = () => {
      setNotificationType(type);
      text ? (dynamicText.current = text) : (dynamicText.current = null);
    };

    if (!loading) {
      setLoading(Date.now());
      op();
    } else {
      // Compute how much time has passed since the previous notification popped up
      const passedTime = Date.now() - loading;

      // Delay in case of queued notification
      setTimeout(
        () => {
          op();
          setLoading(0);
        },
        // Animation time substracted from the passed time
        // Extra delay is needed cause the next notification will be cleared immediately after the previous one is finished
        7000 - passedTime + extraDelay
      );
    }
  };

  return [setNotification, { props: notifyProps, clear }] as const;
};
