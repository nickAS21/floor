import { cn } from '@/shared/lib/utils';
import { Notification, NotificationProps } from '@mantine/core';

export type NotifyProps = {
  kind: "success" | "fail";
  text: string;
  visible: boolean;
  onClose: () => void;
} & Omit<NotificationProps, 'onClose'>;
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
    {...props}
      onClose={onClose}
      pos='fixed'
      bottom={12}
      left={12}
      classNames={{ root: "z-10" }}
    >
      <p className={cn('mr-8 text-base', kind === "success" ? 'text-lime-500' : "text-red-500")}>{text}</p>
      <div className='absolute bottom-0 left-0 h-1 w-full'>
        <span
          role="progressbar"
          className={cn(
            kind === "success" ? 'bg-lime-500' : "bg-red-500",
            'block h-full origin-left animate-[progress_7s_ease]'
          )}
          onAnimationEnd={() => {
            onClose();
          }}
        ></span>
      </div>
    </Notification>
  );
}