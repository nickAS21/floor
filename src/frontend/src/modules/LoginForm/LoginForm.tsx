"use client";

import { cn } from "@/shared/lib/utils";
import { Button, TextInput } from "@mantine/core";
import { useForm, Form } from "@mantine/form";

import classes from "./classes.module.css";

export default function LoginForm() {
    const form = useForm({
        initialValues: {
            email: '',
            password: ''
        }
    });

    return <>
        <Form form={form} className="flex flex-col items-center justify-center h-screen">
            <div className={cn("p-4 rounded-md max-w-[414px] w-full", classes.boxShadow)}>
                <hgroup className="flex flex-col items-center mb-4">
                    <h1 className="text-xl font-semibold">Smart Tuya and Solarman Control</h1>
                    <p className="text-center text-sm text-gray-700">Secure Login to Manage Your Smart Home & Solar Energy</p>
                </hgroup>
                <TextInput label="Email" {...form.getInputProps("email")} classNames={{ input: classes.formInput }} />
                <TextInput label="Password" {...form.getInputProps("password")} classNames={{ input: classes.formInput, root: "mt-3" }} />
                <Button fullWidth type="submit" variant="default" classNames={{ root: "mt-6 bg-[#b6ff6b] transition hover:bg-[#b1ff62] text-black hover:text-white rounded-full" }}>Log in</Button>
            </div>
        </Form>
    </>
}