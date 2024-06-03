import { Container } from "@mantine/core";
import { Metadata } from "next";

export const metadata: Metadata = {
    title: "Tuya's main page",
    description: "Basically, there is nothing here."
}

type Props = {
    children: React.ReactNode
}
export default function Layout({ children }: Props) {
    return (
        <Container>{children}</Container>
    )
}