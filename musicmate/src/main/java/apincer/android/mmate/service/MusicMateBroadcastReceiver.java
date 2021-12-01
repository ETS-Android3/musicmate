package apincer.android.mmate.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
//import android.os.Bundle;

import java.util.List;

import apincer.android.mmate.utils.BitmapHelper;
import timber.log.Timber;

/**
 * Supporting android standard broadcast
 *  - Google Music
 *  - Foobar200
 *  - Neutron Player
 *  - USB Audio Player Pro
 *  - VIVO Music Player, cannot get player details
 *  - FIIO Music Player
 *
 *  NOT supported player - not send any broadcast at all
 *      - HFPlayer (com.onkyo.jp.musicplayer)
 *      - Radsone DCT
 *      - Hiby Music
 */
public class MusicMateBroadcastReceiver extends BroadcastReceiver implements ListeningReceiver {
    //private MusicListeningService service;
    protected String title;
    protected String artist;
    protected String album;
    protected MusicPlayerInfo playerInfo;
    public static String DEAFULT_PLAYER_NAME = "UNKNOWN Player";

    public MusicMateBroadcastReceiver( ) {
       // this.service = service;
        playerInfo = new MusicPlayerInfo();
        initPlayer();
    }

    private void initPlayer() {
        playerInfo.playerName = DEAFULT_PLAYER_NAME;
        playerInfo.playerPackage = "unknown";
        playerInfo.playerIconBitmap = null;
        playerInfo.playerIconDrawable = null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //KEEP for DEBUG
           /* Bundle extras = intent.getExtras();
            String string = "";
            if(extras != null) {
                for (String key : extras.keySet()) {
                    string += " " + key + " => " + extras.get(key) + ";";
                }
            } */

            boolean paused = intent.getBooleanExtra("paused", false);
            if(!paused) {
                try {
                    extractPlayer(context, intent);
                    extractTitle(intent);
                   // displayNotification();
                    MusicListeningService.getInstance().setPlayerInfo(playerInfo);
                    MusicListeningService.getInstance().setPlayingSong(title,artist,album);
                   // service.setListeningReceiver(this);
                }catch (Exception ex) {
                    Timber.e(ex);
                }
            }
    }

    protected void extractPlayer(Context context, Intent intent) {
        String action = intent.getAction();

        if(action.startsWith(PREFIX_UAPP)) {
            setPlayer(context, PACKAGE_UAPP,null);
        }else {
            String playerPackage = intent.getStringExtra(INTENT_KEY_PACKAGE);
            String playerName = intent.getStringExtra(INTENT_KEY_PLAYER);
            if(PLAYER_NAME_FOOBAR2000.equalsIgnoreCase(playerName)) {
                playerPackage = PACKAGE_FOOBAR2000;
            }else if(PACKAGE_POWERAMP.equals(intent.getStringExtra(INTENT_KEY_SCROBBLING_SOURCE))) {
                playerPackage = PACKAGE_POWERAMP;
            }

            if(playerPackage==null) {
                playerPackage = getDefaultPlayerPackage(context);
            }
            setPlayer(context, playerPackage,playerName);
        }
    }

    protected String getDefaultPlayerPackage(Context context) {
        try {
            Intent musicPlayerIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC);
            List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(musicPlayerIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if(list!=null && list.size()>0) {
                // get last in the list
                return list.get(list.size()-1).activityInfo.packageName;
            }
        }catch (Exception ex) {
            Timber.e(ex);
        }
        return null;
    }

    protected void setPlayer(Context context, String packageName, String playerName) {
        initPlayer();
        playerInfo.playerPackage = packageName;
        playerInfo.playerName = playerName==null?DEAFULT_PLAYER_NAME:playerName;
        ApplicationInfo ai = MusicListeningService.getInstance().getApplicationInfo(packageName);
        if(ai!=null) {
            playerInfo.playerIconDrawable = context.getPackageManager().getApplicationIcon(ai);
                if (playerInfo.playerIconDrawable != null) {
                    playerInfo.playerIconBitmap = BitmapHelper.drawableToBitmap(playerInfo.playerIconDrawable);
                }else {
                    playerInfo.playerIconBitmap = null;
                }
                if(playerName==null) {
                    playerInfo.playerName = String.valueOf(context.getPackageManager().getApplicationLabel(ai));
                }
        }
    }

    public void register(Context context) {
        IntentFilter iF = new IntentFilter();

        // USB Audio Player Pro
        iF.addAction("com.extreamsd.usbaudioplayershared.metachanged"); // API < 14
        //iF.addAction("com.extreamsd.usbaudioplayershared.playstatechanged"); // API >= 14, no need
		
		//PowerAmp
        iF.addAction("com.maxmpz.audioplayer.playstatechanged");
		iF.addAction("com.maxmpz.audioplayer.metachanged");

        // Sony
        iF.addAction("com.sonyericson.music.metachanged");

        //VIVO Music
         iF.addAction("com.android.bbkmusic.metachanged");

         //MIUI
        iF.addAction("com.miui.player.metachanged");

        //HTC
       // iF.addAction("com.htc.music.metachanged");
       // iF.addAction("com.htc.music.playstatechanged");

        // GoneMAD
        iF.addAction("gonbemad.dashclock.music.metachanged");
        iF.addAction("gonbemad.dashclock.music.playstatechanged");

        //JR
        iF.addAction("com.jrstudio.music.metachanged");
        iF.addAction("com.jrstudio.music.playstatechanged");

        // SAMSUNG
        iF.addAction("com.samsung.sec.android.MusicPlayer.metachanged");
        iF.addAction("com.samsung.music.metachanged");
        iF.addAction("com.samsung.sec.metachanged");
        iF.addAction("com.samsung.sec.android.metachanged");
        iF.addAction("com.samsung.MusicPlayer.metachanged");
        iF.addAction("com.sec.android.music.state.META_CHANGED");

        // FIIO
        iF.addAction("com.fiio.musicalone.player.brocast");

        // hiby
        iF.addAction("com.hiby.music");

        //Audio
       // iF.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
       // iF.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
      //  iF.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
	  
	    // Android
        iF.addAction("com.android.music.metachanged");
        iF.addAction("com.android.music.playstatechanged");
        iF.addAction("com.android.music.playbackcomplete");
        iF.addAction("com.android.music.queuechanged");
        iF.addAction("com.android.music.updateprogress");

        context.registerReceiver(this, iF);
    }

    protected void extractTitle(Intent intent) {
       // String action = intent.getAction();
       artist = intent.getStringExtra("artist");
       album = intent.getStringExtra("album");
       title = intent.getStringExtra("track");
       //if(PACKAGE_POWERAMP.equals(playerPackage)) {
       //    title = StringUtils.trimToEmpty(title.replace( " - "+artist+" - "+ album, ""));
       //}
    }
/*
    protected final void displayNotification() {
        service.setListeningSong(title, artist,album);
    }

    @Override
    public MusicPlayerInfo getPlayerInfo() {
        return playerInfo;
    } */
}
