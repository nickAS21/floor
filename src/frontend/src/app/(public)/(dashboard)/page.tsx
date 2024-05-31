import MainDisplay from "@/modules/MainDisplay";
import { Metadata } from "next";

export const metadata: Metadata = {
    title: "Tuya's main page",
    description: "Basically, there is nothing here."
}

export default function Page() {
    return (
        <MainDisplay />
    );
}