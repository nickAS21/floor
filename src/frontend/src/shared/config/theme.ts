import { createTheme } from '@mantine/core';

// Define your own theme
const theme = createTheme({
    colors: {
        light: [
            "#FFFFFF", 
            "#F5F2F7", 
            "#E1DEE3", 
            "#F0EDF3", 
            "#CFCDD1", 
            "#B8B6BA", 
            "#A4A2A6", 
            "#908E91", 
            "#7C7A7D", 
            "#686669"
        ],
        dark: [
            "#2D3250",
            "#424769",
            "#7077A1",
            "#F6B17A",
            "#000000", 
            "#282828", 
            "#3C3C3C", 
            "#141414", 
            "#505050", 
            "#646464", 
        ]
    }
});

export default theme;