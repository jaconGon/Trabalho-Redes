package com.redes;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) {
        RecebeDados rd = new RecebeDados();
        rd.start();

        Semaphore sem = new Semaphore(3);
        EnviaDados ed1 = new EnviaDados(sem, "envia");
        EnviaDados ed2 = new EnviaDados(sem, "ack");

        ed2.start();
        ed1.start();

        try {
            ed1.join();
            ed2.join();

            rd.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}