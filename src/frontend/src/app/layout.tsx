import { ColorSchemeScript, MantineProvider } from '@mantine/core';

import '@mantine/core/styles.css';

import theme from '@/shared/config/theme';
import "./globals.css";
import { StoreProvider } from "@/shared/config/StoreProvider";

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <head>
        <ColorSchemeScript />
      </head>
      <body>
        <StoreProvider>
          <MantineProvider theme={theme}>{children}</MantineProvider>
        </StoreProvider>
      </body>
    </html>
  );
}
