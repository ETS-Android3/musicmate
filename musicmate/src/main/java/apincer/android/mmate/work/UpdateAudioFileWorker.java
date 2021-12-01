package apincer.android.mmate.work;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import apincer.android.mmate.Constants;
import apincer.android.mmate.R;
import apincer.android.mmate.objectbox.AudioTag;
import apincer.android.mmate.repository.AudioFileRepository;
import apincer.android.mmate.service.BroadcastData;

public class UpdateAudioFileWorker extends Worker {
    private UpdateAudioFileWorker(
            @NonNull Context context,
            @NonNull WorkerParameters parameters) {
        super(context, parameters);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data inputData = getInputData();
        String s =inputData.getString(Constants.KEY_MEDIA_TAG_LIST);
        String artworkPath = inputData.getString(Constants.KEY_COVER_ART_PATH);
        Gson gson = new Gson();
        Type listOfAudioTag = new TypeToken<List<AudioTag>>(){}.getType();
        List<AudioTag> files = gson.fromJson(s, listOfAudioTag);
        int pendingTotal = files.size();
        int successCount = 0;
        int errorCount = 0;
        for(AudioTag tag: files) {
            boolean status = AudioFileRepository.getInstance(getApplicationContext()).saveAudioFile(tag, artworkPath);
            String txt = status?getApplicationContext().getString(R.string.alert_write_tag_success, tag.getTitle()):getApplicationContext().getString(R.string.alert_write_tag_fail, tag.getTitle());
            if(status) {
                successCount++;
            }else {
                errorCount++;
            }
            BroadcastData data = new BroadcastData()
                    .setCommand(Constants.COMMAND_SAVE)
                    .setTotalItems(pendingTotal)
                    .setCountError(errorCount)
                    .setCountSuccess(successCount)
                    .setStatus(status?BroadcastData.Status.COMPLETED: BroadcastData.Status.ERROR)
                    .setTagInfo(tag)
                    .setMessage(txt);
            sendBroadcast(data);
        }

        return Result.success();
    }

    protected void sendBroadcast(final BroadcastData data){
        Intent intent = data.getIntent();
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    public static void startWorker(Context context, List<AudioTag> files, String artworkPath) {
        Gson gson = new Gson();
        Type listOfAudioTag = new TypeToken<List<AudioTag>>(){}.getType();
        String s = gson.toJson(files, listOfAudioTag);
        Data inputData = (new Data.Builder())
                .putString(Constants.KEY_MEDIA_TAG_LIST, s)
                .putString(Constants.KEY_COVER_ART_PATH, artworkPath)
                .build();
        WorkRequest workRequest = new OneTimeWorkRequest.Builder(UpdateAudioFileWorker.class)
                .setInputData(inputData).build();
        WorkManager.getInstance(context).enqueue(workRequest);
    }
}
