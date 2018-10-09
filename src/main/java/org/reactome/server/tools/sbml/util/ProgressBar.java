package org.reactome.server.tools.sbml.util;


import java.util.concurrent.TimeUnit;

/**
 * Custom progress bar for SBML exporter that supports running on a thread to keep the timer going when
 * the conversion of a given pathway takes a long time. Note: Thread feature not used for the time being
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 */
public class ProgressBar implements Runnable {

    private static final int width = 70;

    private Thread t;

    private Long start;

    private String species;
    private String current = "";
    private Integer done = 0;
    private Integer total;

    public ProgressBar(String species, Integer total) {
        this.start = System.currentTimeMillis();
        this.species = species;
        this.total = total;
    }

    /**
     * Simple method that prints a progress bar to command line
     *
     * @param current name of the processed element
     * @param done    number of entries added to the graph
     */
    public void update(String current, int done) {
        this.current = current;
        this.done = done;

        current = (total == done) ? "done" : "current:" + current;

        String format = "\r%-30s%s  %3d%% %s %c [%s]";

        double percent = (double) done / total;
        StringBuilder progress = new StringBuilder(width);
        progress.append('|');
        int i = 0;
        for (; i < (int) (percent * width); i++) progress.append("=");
        for (; i < width; i++) progress.append(" ");
        progress.append('|');
        char[] rotators = {'|', '/', '—', '\\'};
        char status = (total == done) ? '>' : rotators[done % rotators.length];
        String time = getTimeFormatted(System.currentTimeMillis() - start);

        System.out.printf(format, species, time, (int) (percent * 100), progress, status, current);
        if (done == total){
            System.out.println();
            t.interrupt();
        }
    }

    public void done() {
        done = total;
        t.interrupt();
        update(current, done);
    }

    @Override
    public void run() {
        try {
            while (t.isAlive()) {
                update(current, done);
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            //Nothing here
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public Thread start() {
        t = new Thread(this);
        t.start();
        return t;
    }

    private static String getTimeFormatted(Long millis) {
        return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
    }
}



