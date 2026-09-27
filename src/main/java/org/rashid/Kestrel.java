package org.rashid;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Kestrel {

    public static void main(String[] args) {
        Kestrel kestrel = new Kestrel();
        kestrel.run();
    }

    public void run() {
        log.info("Starting kestrel server");
    }
}
