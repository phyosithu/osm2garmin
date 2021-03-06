/*
 * #%L
 * Osm2garminAPI
 * %%
 * Copyright (C) 2011 Frantisek Mantlik <frantisek at mantlik.cz>
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package org.mantlik.osm2garmin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import jbittorrentapi.DataVerifier;
import jbittorrentapi.TorrentFile;
import jbittorrentapi.Utils;
import org.openide.util.Exceptions;
import org.xml.sax.SAXException;

/**
 *
 * @author fm
 */
public class PlanetUpdateDownloader extends ThreadProcessor {

    private static final DecimalFormat DF = new DecimalFormat("0");
    public TorrentDownloader torrentDownloader = null;
    int startPiece = 0;
    int noOfPieces = 0;
    int firstPieceToProcess = 0;

    /**
     *
     * @param parameters
     */
    public PlanetUpdateDownloader(Properties parameters) {
        super(parameters);
    }

    @Override
    public void run() {
        if (parameters.getProperty("skip_planet_update", "false").equals("true")) {
            setProgress(100);
            setStatus("Skipped.");
            setState(COMPLETED);
            synchronized (this) {
                notify();
            }
            return;
        }
        String osmosiswork = parameters.getProperty("osmosiswork");
        File osmosisState = new File(osmosiswork, "state.txt");
        File osmosisStateBackup = new File(osmosiswork, "state_old.txt");

        // recover or make planet file backup
        File planetFile = new File(parameters.getProperty("planet_file"));
        File oldPlanetFile = new File(parameters.getProperty("old_planet_file"));
        if (oldPlanetFile.exists() && (!planetFile.exists() || (planetFile.length() <= oldPlanetFile.length()))) {
            // recover backup
            if (planetFile.exists()) {
                planetFile.delete();
            }
            if (osmosisStateBackup.exists()) {
                if (osmosisState.exists()) {
                    osmosisState.delete();
                }
                try {
                    Utilities.copyFile(osmosisStateBackup, osmosisState);
                } catch (IOException ex) {
                    Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE,
                            "Error recovering Osmosis status backup.", ex);
                    setState(ERROR);
                    synchronized (this) {
                        notify();
                    }
                    return;
                }
            }
        } else {
            // backup planet
            if (oldPlanetFile.exists() && planetFile.exists()) {
                if (parameters.containsKey("planet_backup")) {
                    File bkp = new File(parameters.getProperty("planet_backup"));
                    if (bkp.exists()) {
                        bkp.delete();
                    }
                    oldPlanetFile.renameTo(bkp);
                } else {
                    oldPlanetFile.delete();
                }
            }
            // make backup
            planetFile.renameTo(oldPlanetFile);
            if (osmosisStateBackup.exists()) {
                osmosisStateBackup.delete();
            }
            if (osmosisState.exists()) {
                try {
                    Utilities.copyFile(osmosisState, osmosisStateBackup);
                } catch (IOException ex) {
                    Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE,
                            "Error creating Osmosis status backup.", ex);
                    setState(ERROR);
                    synchronized (this) {
                        notify();
                    }
                    return;
                }
            }
        }

        // Download planet updates
        setStatus("Searching for updates.");
        int doDownload = 0;
        TorrentFile torrent = null;
        while ((noOfPieces < 1) && (doDownload < 3)) {
            torrent = createUpdatesTorrent(osmosisState);
            if (torrent == null) {
                setStatus("Error creating updates pseudo-torrent.");
                setState(ERROR);
                synchronized (this) {
                    notify();
                }
                return;
            }
            doDownload++;
        }
        if (noOfPieces < 1) {
            setStatus("Nothing to download.");
            setState(COMPLETED);
            synchronized (this) {
                notify();
            }
            return;
        }
        torrentDownloader
                = new TorrentDownloader(parameters, torrent, new File(Utilities.getUserdir(this)),
                startPiece, noOfPieces, new UpdateFileVerifier(torrent));
        Utilities.getInstance().addProcessToMonitor(torrentDownloader);
        torrentDownloader.changeSupport.addPropertyChangeListener(this);
        while (torrentDownloader.getState() != TorrentDownloader.COMPLETED) {
            if (torrentDownloader.getState() == Downloader.ERROR) {
                setState(ERROR);
                synchronized (this) {
                    notify();
                }
                return;
            }
            try {
                setStatus(torrentDownloader.getStatus());
                setProgress(torrentDownloader.getProgress());
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                //Logger.getLogger(PlanetDownloader.class.getName()).log(Level.SEVERE, null, ex);
                setState(ERROR);
                synchronized (this) {
                    notify();
                }
                return;
            }
        }
        int sequence = 0;
        String[] args;
        ArrayList<String> l = new ArrayList<String>();
        int ii = 0;
        int iii = 0;
        int fileno;
        // process downloads with Osmosis - 24 files in a single pass
        for (fileno = firstPieceToProcess; fileno < (startPiece + noOfPieces); fileno++) {
            setProgress((float) (100.0 * (fileno - firstPieceToProcess)
                    / (startPiece + noOfPieces - firstPieceToProcess + 24)));
            setStatus("Processing updates (" + ((int) getProgress()) + " %)...");
            ii++;
            l.add("--rxc");
            l.add("file=" + Utilities.getUserdir(this) + Utilities.updateName(fileno));
            l.add("--buffer-change");
            l.add("bufferCapacity=10000");
            if (ii == 24) {
                l.add("--apc");
                l.add("sourceCount=24");
                l.add("--sc");
                l.add("--simc");
                l.add("--wxc");
                l.add("file=" + Utilities.getUserdir(this) + "update" + sequence + ".osc.gz");
                args = l.toArray(new String[0]);
                try {
                    Utilities.getInstance().runExternal("org.openstreetmap.osmosis.core.Osmosis", "run", "osmosis",
                            args, this);
                } catch (Exception ex) {
                    setStatus(ex.getMessage());
                    setState(ERROR);
                    synchronized (this) {
                        notify();
                    }
                    return;
                }
                iii++;
                if (iii == 30) {  // collected 30 files, process then in an extra step
                    ArrayList<String> ll = new ArrayList<String>();
                    setStatus("Processing updates (" + ((int) getProgress()) + " %)... merging month #" + (sequence - 28) + " updates");
                    for (int seq2 = sequence - 29; seq2 <= sequence; seq2++) {
                        ll.add("--rxc");
                        ll.add("file=" + Utilities.getUserdir(this) + "update" + seq2 + ".osc.gz");
                        ll.add("--buffer-change");
                        ll.add("bufferCapacity=10000");
                    }
                    ll.add("--apc");
                    ll.add("sourceCount=30");
                    ll.add("--sc");
                    ll.add("--simc");
                    ll.add("--wxc");
                    ll.add("file=" + Utilities.getUserdir(this) + "update" + (sequence + 1) + ".osc.gz");
                    args = ll.toArray(new String[0]);
                    try {
                        Utilities.getInstance().runExternal("org.openstreetmap.osmosis.core.Osmosis", "run", "osmosis",
                                args, this);
                    } catch (Exception ex) {
                        setStatus(ex.getMessage());
                        setState(ERROR);
                        synchronized (this) {
                            notify();
                        }
                        return;
                    }
                    for (int seq2 = sequence - 29; seq2 <= sequence; seq2++) {
                        boolean deleted = new File(Utilities.getUserdir(this) + "update" + seq2 + ".osc.gz").delete();
                        if (!deleted) {
                            setStatus("Cannot delete " + "update" + seq2 + ".osc.gz");
                            setState(ERROR);
                            synchronized (this) {
                                notify();
                            }
                            return;
                        }
                    }
                    boolean renamed = new File(Utilities.getUserdir(this) + "update" + (sequence + 1) + ".osc.gz")
                            .renameTo(new File(Utilities.getUserdir(this) + "update" + (sequence - 29) + ".osc.gz"));
                    if (!renamed) {
                        setStatus("Cannot rename " + "update" + (sequence+1) + ".osc.gz to update" + (sequence-29) + ".osc.gz");
                        setState(ERROR);
                        synchronized (this) {
                            notify();
                        }
                        return;
                    }
                    sequence -= 29;
                    iii = 0;
                }
                sequence++;
                l.clear();
                ii = 0;
            }
        }
        if (!l.isEmpty()) {
            setProgress((float) (100.0 * (fileno - firstPieceToProcess)
                    / (startPiece + noOfPieces - firstPieceToProcess + 24)));
            l.add("--apc");
            l.add("sourceCount=" + ii);
            l.add("--sc");
            l.add("--simc");
            l.add("--wxc");
            l.add("file=" + Utilities.getUserdir(this) + "update" + sequence + ".osc.gz");
            args = l.toArray(new String[0]);
            try {
                Utilities.getInstance().runExternal("org.openstreetmap.osmosis.core.Osmosis", "run", "osmosis",
                        args, this);
            } catch (Exception ex) {
                setStatus(ex.getMessage());
                setState(ERROR);
                synchronized (this) {
                    notify();
                }
                return;
            }
        }
        // download latest osmosiswork/state.txt
        setStatus("Downloading current state.txt");
        String[] mirrors = parameters.getProperty("planet_file_update_urls").split(",");
        boolean ok = false;
        while (!ok) {
            String mirror = mirrors[((int) (Math.random() * 1.0 * mirrors.length))];
            if (!mirror.endsWith("/")) {
                mirror += "/";
            }
            String stateUrl = (mirror + Utilities.updateName(startPiece + noOfPieces - 1)).replace(".osc.gz", ".state.txt");
            URL surl;
            try {
                surl = new URL(stateUrl);
            } catch (MalformedURLException ex) {
                Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE, "", ex);
                setStatus(ex.getMessage());
                setState(ERROR);
                synchronized (this) {
                    notify();
                }
                return;
            }
            String state = osmosiswork + "state.txt";
            new File(state).delete();
            Downloader downloader = new Downloader(surl, state);
            while (downloader.getStatus() != Downloader.COMPLETE) {
                if (downloader.getStatus() == Downloader.ERROR) {
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    //Logger.getLogger(PlanetUpdateDownloader.class.getName()).log(Level.SEVERE, null, ex);
                    //setState(ERROR);
                    setStatus("Interrupted.");
                    setState(ERROR);
                    synchronized (this) {
                        notify();
                    }
                    return;
                }
            }
            if (downloader.getStatus() == Downloader.COMPLETE) {
                ok = true;
            }
        }
        setProgress(100);
        setStatus("Completed.");
        setState(COMPLETED);
        synchronized (this) {
            notify();
        }
    }

    private class UpdateFileVerifier implements DataVerifier {

        public UpdateFileVerifier(TorrentFile torrent) {
        }

        /*
         * Verify gzip file readibility
         */
        @Override
        public boolean verify(int index, byte[] data) {
            InputStream is = null;
            File hashfile = new File(Utilities.getUserdir(PlanetUpdateDownloader.this) + Utilities.updateName(index) + ".sha1");
            if (hashfile.exists()) {
                int l = (int) hashfile.length();
                byte[] hexhash = new byte[l];
                try {
                    is = new FileInputStream(hashfile);
                    is.read(hexhash);
                    is.close();
                } catch (IOException ex) {
                    hashfile.delete();
                    return false;
                }
                boolean result = Utils.byteArrayToByteString(Utils.hash(data)).
                        matches(Utils.byteArrayToByteString(Utils.hexStringToByteArray(new String(hexhash))));
                if (! result) {
                    hashfile.delete();
                    File datafile = new File(Utilities.getUserdir(PlanetUpdateDownloader.this) + Utilities.updateName(index));
                    if (datafile.exists()) {
                        datafile.delete();
                    }
                }
                return result;
            }
            try {
                is = new GZIPInputStream(new ByteArrayInputStream(data));
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                db.parse(is);
                is.close();
            } catch (SAXException ex) {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ex1) {
                        return false;
                    }
                }
                return false;
            } catch (ParserConfigurationException ex) {
                setStatus(ex.getMessage());
                setState(ERROR);
                try {
                    is.close();
                } catch (IOException ex1) {
                    return false;
                }
                return false;
            } catch (IOException ex) {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ex1) {
                        return false;
                    }
                }
                return false;
            }
            try {
                OutputStream os = new FileOutputStream(hashfile);
                os.write(Utils.bytesToHex(Utils.hash(data)).getBytes());
                os.close();
            } catch (IOException ex) {
                Logger.getLogger(Osm2garmin.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
            return true;
        }
    }

    /*
     * Source:
     * http://stackoverflow.com/questions/263013/java-urlconnection-how-could-i-find-out-a-files-size
     */
    private long tryGetFileSize(URL url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.getInputStream();
            return conn.getContentLength();
        } catch (IOException e) {
            return -1;
        } finally {
            conn.disconnect();
        }
    }

    /*
     * Create pseudo-torrent structure for download of updates
     * each hourly update file serves as a (variable-length) piece
     * and downloads latest state.txt to osmosiswork.
     * In addition, sets startPiece and noOfPieces according to the
     * Planet database and local planet state.
     */
    private TorrentFile createUpdatesTorrent(File osmosisstate) {
        TorrentFile torrent = new TorrentFile();
        torrent.announceURL = "http://www.mantlik.cz:80/tracker/announce.php";
        torrent.comment = "Original data from http://planet.openstreetmap.org/ "
                + "(C) OpenStreetMap contributors";
        torrent.createdBy = "Osm2garmin 1.1";
        torrent.creationDate = System.currentTimeMillis() / 1000;
        ArrayList<String> tier = new ArrayList<String>();
        tier.add("http://www.mantlik.cz:80/tracker/announce.php");
        torrent.announceList.add(tier);
        tier = new ArrayList<String>();
        tier.add("http://tracker.ipv6tracker.org:80/announce");
        tier.add("udp://tracker.ipv6tracker.org:80/announce");
        torrent.announceList.add(tier);
        tier = new ArrayList<String>();
        tier.add("udp://tracker.publicbt.com:80/announce");
        tier.add("http://tracker.publicbt.com:80/announce");
        torrent.announceList.add(tier);
        tier = new ArrayList<String>();
        tier.add("udp://tracker.openbittorrent.com:80/announce");
        torrent.announceList.add(tier);
        tier = new ArrayList<String>();
        tier.add("http://open-tracker.appspot.com/announce");
        torrent.announceList.add(tier);
        torrent.changeAnnounce();
        String[] mirrors = parameters.getProperty("planet_file_update_urls").split(",");
        torrent.urlList.addAll(Arrays.asList(mirrors));
        torrent.info_hash_as_binary = Utils.hash("Osm2Garmin ODbL planet update pseudo-torrent".getBytes());
        torrent.info_hash_as_hex = Utils.byteArrayToByteString(
                torrent.info_hash_as_binary);
        torrent.info_hash_as_url = Utils.byteArrayToURLString(
                torrent.info_hash_as_binary);
        int sequence = -1;
        try {
            sequence = Utilities.getSequenceNo(osmosisstate) + 1;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        if (sequence < 0) {
            return null;
        }
        startPiece = sequence;
        String mirror = mirrors[((int) (Math.random() * 1.0 * mirrors.length))];
        if (!mirror.endsWith("/")) {
            mirror += "/";
        }
        torrent.total_length = 0;
        torrent.setPieceLength(sequence, 0);
        int size = 1;
        firstPieceToProcess = sequence;
        // search for existing older files to seed
        while (size > 0) {
            sequence--;
            String fname = Utilities.getUserdir(this) + Utilities.updateName(sequence);
            size = (int) (new File(fname).length());
        }
        sequence++;
        startPiece = sequence;
        while (sequence < firstPieceToProcess) {
            String fname = Utilities.getUserdir(this) + Utilities.updateName(sequence);
            String hashname = fname + ".sha1";
            if (!new File(hashname).exists()) {
                setStatus("Checking existing " + Utilities.updateName(sequence));
                String url = mirror + Utilities.updateName(sequence);
                try {
                    size = (int) tryGetFileSize(new URL(url));
                } catch (MalformedURLException ex) {
                    startPiece = sequence + 1;
                }
            } else {
                size = (int) (new File(fname).length());
            }
            torrent.setPieceLength(sequence, size);
            byte[] temp = Utils.hash(("Piece " + sequence + " length " + size).getBytes());
            torrent.piece_hash_values_as_binary.put(sequence, temp);
            torrent.piece_hash_values_as_hex.put(sequence, Utils.byteArrayToByteString(
                    temp));
            torrent.piece_hash_values_as_url.put(sequence, Utils.byteArrayToURLString(
                    temp));
            torrent.length.add(((long) size));
            torrent.total_length += size;
            torrent.name.add(Utilities.updateName(sequence));
            sequence++;
        }
        sequence = firstPieceToProcess;
        size = 1;
        while (size > 0) {
            setStatus("Searching for updates - " + Utilities.updateName(sequence));
            String url = mirror + Utilities.updateName(sequence);
            try {
                size = (int) tryGetFileSize(new URL(url));
            } catch (MalformedURLException ex) {
                return null;
            }
            if (size < 0) {
                break;
            }
            torrent.setPieceLength(sequence, size);
            byte[] temp = Utils.hash(("Piece " + sequence + " length " + size).getBytes());
            torrent.piece_hash_values_as_binary.put(sequence, temp);
            torrent.piece_hash_values_as_hex.put(sequence, Utils.byteArrayToByteString(
                    temp));
            torrent.piece_hash_values_as_url.put(sequence, Utils.byteArrayToURLString(
                    temp));
            torrent.length.add(((long) size));
            torrent.total_length += size;
            torrent.name.add(Utilities.updateName(sequence));
            sequence++;
        }
        noOfPieces = torrent.name.size();

        return torrent;
    }
}
