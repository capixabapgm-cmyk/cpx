package br.com.minhasrifas;

final class NotificationSchedule {
    static boolean run(Runnable schedule){
        try{schedule.run();return true;}
        catch(SecurityException | IllegalStateException | IllegalArgumentException restricted){return false;}
    }
}
