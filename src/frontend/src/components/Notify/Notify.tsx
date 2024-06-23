import { cn } from "@/shared/lib/utils";
import { Notification, NotificationProps } from "@mantine/core";

export type NotifyProps = {
  kind: "success" | "fail";
  text: string;
  visible: boolean;
  onClose: () => void;
  classNames?: NotificationProps["classNames"] & { progress?: string };
} & Omit<NotificationProps, "onClose" | "classNames">;
export default function Notify({
  kind,
  visible,
  text,
  onClose,
  ...props
}: NotifyProps) {
  if (!visible) return null;

  return (
    <Notification
      classNames={{ root: "z-10" }}
      {...props}
      onClose={onClose}
      pos="fixed"
      bottom={12}
      left={12}
    >
      <p className="mr-8 text-base">{text}</p>
      <div className="absolute bottom-0 left-0 h-1 w-full">
        <span
          role="progressbar"
          className={cn(
            "block h-full origin-left animate-[progress_7s_ease]",
            props.classNames?.progress
          )}
          onAnimationEnd={() => {
            onClose();
          }}
        ></span>
      </div>
    </Notification>
  );
}
