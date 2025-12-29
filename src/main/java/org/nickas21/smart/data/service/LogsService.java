package org.nickas21.smart.data.service;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import org.nickas21.smart.data.dataEntity.BatteryCellInfo;
import org.nickas21.smart.tuya.TuyaDeviceService;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class LogsService {

    private final TuyaDeviceService deviceService;

    public LogsService(TuyaDeviceService deviceService) {
        this.deviceService = deviceService;
    }

    public String getLogsDacha() throws IOException {
        int tailLines = this.deviceService.getLogsDachaLimit();

        // Перевірка наявності змінної оточення K8s для локального тестування
        if (System.getenv("KUBERNETES_SERVICE_HOST") == null) {
            return """
                2025-12-29T18:35:57.864+02:00  INFO 34372 --- [0.0-8084-exec-1] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring DispatcherServlet 'dispatcherServlet'
                2025-12-29T18:35:57.864+02:00  INFO 34372 --- [0.0-8084-exec-1] o.s.web.servlet.DispatcherServlet        : Initializing Servlet 'dispatcherServlet'
                2025-12-29T18:35:57.865+02:00  INFO 34372 --- [0.0-8084-exec-1] o.s.web.servlet.DispatcherServlet        : Completed initialization in 1 ms
                2025-12-29T18:35:58.257+02:00  WARN 34372 --- [0.0-8084-exec-3] o.n.smart.data.dataEntity.DataHome       : DataHomeDacha [DataHome(golegoPowerDefault=42.0, ...)]
                """;
        }

        try {
            ApiClient client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);
            CoreV1Api api = new CoreV1Api();

            String labelSelector = "app=smart-solar-tuya";
            String namespace = "default";

            V1PodList podList = api.listNamespacedPod(namespace)
                    .labelSelector(labelSelector)
                    .execute();

            if (podList.getItems() == null || podList.getItems().isEmpty()) {
                return "Error: No pods found for selector: " + labelSelector;
            }

            V1Pod pod = podList.getItems().get(0);

            // Виклик з обмеженням кількості рядків від краю (tailLines)
            return api.readNamespacedPodLog(pod.getMetadata().getName(), namespace)
                    .tailLines(tailLines)
                    .execute();

        } catch (ApiException e) {
            return "Kube API Error: " + e.getResponseBody();
        }
    }

    public BatteryCellInfo getLogsGolego() throws IOException {
        // Твоя логіка для Golego
        return new BatteryCellInfo();
    }
}