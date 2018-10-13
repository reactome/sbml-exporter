package org.reactome.server.tools.sbml.util;


import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Custom progress bar for SBML exporter that supports running on a thread to keep the timer going when
 * the conversion of a given pathway takes a long time. Note: Thread feature not used for the time being
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 */
public class ProgressBar extends TimerTask {

    private static final int width = 70;

    private Timer timer;
    private Long start;
    private Boolean verbose;

    private String species;
    private String current = "";
    private Integer done = 0;
    private Integer total;

    public ProgressBar(String species, Integer total, Boolean verbose) {
        this.start = System.currentTimeMillis();
        this.species = species;
        this.total = total;
        this.verbose = verbose;
    }

    /**
     * Simple method that prints a progress bar to command line
     *
     * @param current name of the processed element
     * @param done    number of entries added to the graph
     */
    public synchronized void update(String current, int done) {
        if (!verbose) return;
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
        if (done == total) {
            System.out.println();
            if (timer != null){
                timer.cancel();
                timer = null;
            }
        }
    }

    public synchronized void interrupt() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public synchronized void done() {
        if (timer != null) {
            timer.cancel();
            timer = null;
            done = total;
            update(current, done);
        }
    }

    @Override
    public void run() {
        update(current, done);
    }

    public void start() {
        timer = new Timer(true);
        timer.schedule(this, 250, 500);
    }

    private static String getTimeFormatted(Long millis) {
        return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
    }
}



