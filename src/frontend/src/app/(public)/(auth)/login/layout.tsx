import { Metadata } from "next";

export const metadata: Metadata = {
    title: "Login",
    description: "Log in to your Smart Tuya and Solarman control cabinet"
}

type Props = {
    children: React.ReactNode;
}

export default function Layout({ children }: Props) {
    return (
        <>{children}</>
    )
}