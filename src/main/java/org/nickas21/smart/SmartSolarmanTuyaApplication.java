package org.nickas21.smart;

import lombok.extern.slf4j.Slf4j;
import org.nickas21.smart.install.SmartSolarmanTuyaServiceInstall;
import org.nickas21.smart.tuya.source.TuyaMessageDataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import java.util.Arrays;

@Slf4j
@SpringBootConfiguration
@ComponentScan({"org.nickas21.smart"})
public class SmartSolarmanTuyaApplication {

    private static final String SPRING_CONFIG_NAME_KEY = "--spring.config.name";
    private static final String DEFAULT_SPRING_CONFIG_PARAM = SPRING_CONFIG_NAME_KEY + "=" + "floor";

    public static void main(String[] args) {
        try {
            SpringApplication application = new SpringApplication(SmartSolarmanTuyaApplication.class);
            ConfigurableApplicationContext context = application.run(updateArguments(args));
            SmartSolarmanTuyaServiceInstall solarmanTuyaServiceInstall = context.getBean(SmartSolarmanTuyaServiceInstall.class);
            TuyaMessageDataSource tuyaMessageDataSource = solarmanTuyaServiceInstall.performInstall();
            if (tuyaMessageDataSource == null) {
//                solarmanTuyaServiceInstall.destroy();
                System.exit(1);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            System.exit(1);
        }
    }

    private static String[] updateArguments(String[] args) {
        if (Arrays.stream(args).noneMatch(arg -> arg.startsWith(SPRING_CONFIG_NAME_KEY))) {
            String[] modifiedArgs = new String[args.length + 1];
            System.arraycopy(args, 0, modifiedArgs, 0, args.length);
            modifiedArgs[args.length] = DEFAULT_SPRING_CONFIG_PARAM;
            return modifiedArgs;
        }
        return args;
    }
}
