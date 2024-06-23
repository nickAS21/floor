import { ColorSchemeScript, MantineProvider } from '@mantine/core';

import { StoreProvider } from "@/shared/config/StoreProvider";
import { NotifyProvider } from '@/shared/context/notifications';

import theme from '@/shared/config/theme';
import '@mantine/core/styles.css';
import "./globals.css";

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <head>
        <ColorSchemeScript defaultColorScheme="light" />
      </head>
      <body>
        <StoreProvider>
          <MantineProvider theme={theme} defaultColorScheme="light">
            <NotifyProvider>
              {children}
            </NotifyProvider>
          </MantineProvider>
        </StoreProvider>
      </body>
    </html>
  );
}
