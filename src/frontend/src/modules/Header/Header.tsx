"use client";

import { Switch, useMantineTheme, rem, useMantineColorScheme, UnstyledButton, Drawer } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { IconSun, IconMoonStars, IconBell } from '@tabler/icons-react';
import { useEffect, useState } from 'react';

export default function Header() {
    const theme = useMantineTheme();
    const { setColorScheme, colorScheme } = useMantineColorScheme();
    const [checked, setChecked] = useState(colorScheme === "dark" ? true : false);
    const [opened, { open, close }] = useDisclosure(false);

    useEffect(() => {
        setColorScheme(checked ? "dark" : "light");
    }, [checked]);

    const sunIcon = (
        <IconSun
            style={{ width: rem(24), height: rem(20) }}
            stroke={2.5}
            color={theme.colors.yellow[5]}
        />
    );

    const moonIcon = (
        <IconMoonStars
            style={{ width: rem(24), height: rem(20) }}
            stroke={2.5}
            color="#fb923c"
        />
    );

    return (
        <header className="header flex justify-between items-center py-4 px-6 border-b-2 border-b-[#e9ecef] dark:border-b-[#343434]">
            <h1>Tuya Dashboard</h1>
            <div className='flex items-center gap-4'>
                <Switch classNames={{ body: "cursor-pointer" }} checked={checked} title="Dark mode" onChange={(event) => setChecked(event.currentTarget.checked)} size="lg" color="dark.5" onLabel={moonIcon} offLabel={sunIcon} />
                <UnstyledButton onClick={open} title="Notifications">
                    <IconBell className='dark:text-orange-400' />
                </UnstyledButton>
                <Drawer opened={opened} onClose={close} title="Notifications" position='right'>

                </Drawer>
            </div>
        </header>
    );
}