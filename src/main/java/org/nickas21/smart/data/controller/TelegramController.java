package org.nickas21.smart.data.controller;

//@RestController
//@RequestMapping(value = "/api")
public class TelegramController {
//
//    @Autowired
//    private TelegramService telegramService;
//
//
//    @PostMapping("/sendNotificationDacha")
//    public Mono<ResponseEntity<String>> sendNotificationDacha(@RequestBody String message) {
//        String houseName = telegramService.getTelegramBotDacha().getHouseName();
//        return telegramService.sendNotification(telegramService.getTelegramBotDacha(), message)
//                .flatMap(isSentMessage -> {
//                    if (isSentMessage) {
//                        return Mono.just(ResponseEntity.ok("Send Notification from [" + houseName + "] is successful"));
//                    } else {
//                        return Mono.error(new Throwable("Invalid send Notification"));
//                    }
//                })
//                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(e.getMessage() == null ? "Invalid password or username from [" + houseName + "]" : e.getMessage())));
//    }
//    @PostMapping("/sendNotificationHomer")
//    public Mono<ResponseEntity<String>> sendNotificationHome(@RequestBody String message) {
//        String houseName = telegramService.getTelegramBotHome().getHouseName();
//        return telegramService.sendNotification(telegramService.getTelegramBotHome(), message)
//                .flatMap(isSentMessage -> {
//                    if (isSentMessage) {
//                        return Mono.just(ResponseEntity.ok("Send Notification from [" + houseName + "] is successful"));
//                    } else {
//                        return Mono.error(new Throwable("Invalid send Notification"));
//                    }
//                })
//                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(e.getMessage() == null ? "Invalid password or username from [" + houseName + "]" : e.getMessage())));
//    }
//
//    @PostMapping("/sendNotificationAlarm")
//    public Mono<ResponseEntity<String>> sendNotificationAlarm(@RequestBody String message) {
//        String houseName = telegramService.getTelegramBotAlarm().getHouseName();
//        return telegramService.sendNotification(telegramService.getTelegramBotAlarm(), message)
//                .flatMap(isSentMessage -> {
//                    if (isSentMessage) {
//                        return Mono.just(ResponseEntity.ok("Send Notification from [" + houseName + "] is successful"));
//                    } else {
//                        return Mono.error(new Throwable("Invalid send Notification"));
//                    }
//                })
//                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(e.getMessage() == null ? "Invalid password or username from [" + houseName + "]" : e.getMessage())));
//    }
}
