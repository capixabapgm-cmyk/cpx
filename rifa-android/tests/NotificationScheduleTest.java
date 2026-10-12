package br.com.minhasrifas;
public final class NotificationScheduleTest {
    public static void main(String[] args){
        if(!NotificationSchedule.run(()->{}))throw new AssertionError("Normal scheduling failed");
        if(NotificationSchedule.run(()->{throw new SecurityException("ACCESS_NETWORK_STATE required");}))throw new AssertionError("Permission restriction escaped");
        if(NotificationSchedule.run(()->{throw new IllegalStateException("Scheduler unavailable");}))throw new AssertionError("Scheduler restriction escaped");
        if(NotificationSchedule.run(()->{throw new IllegalArgumentException("Job rejected");}))throw new AssertionError("Job restriction escaped");
        System.out.println("Inicialização: restrições de permissão e de agendamento não interrompem o aplicativo.");
    }
}
