package com.xpn.spellnote.ui.dictionary;

import androidx.databinding.Bindable;
import androidx.annotation.StringRes;

import com.google.firebase.perf.metrics.AddTrace;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.xpn.spellnote.BR;
import com.xpn.spellnote.R;
import com.xpn.spellnote.models.DictionaryModel;
import com.xpn.spellnote.models.WordModel;
import com.xpn.spellnote.services.dictionary.SavedDictionaryService;
import com.xpn.spellnote.services.word.SavedWordsService;
import com.xpn.spellnote.ui.BaseViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import timber.log.Timber;


public class LanguageItemVM extends BaseViewModel {

    private DictionaryModel dictionaryModel;
    private Status status;
    private int progress;
    private FileDownloadTask downloadTask;
    private final ViewContract viewContract;
    private final SavedDictionaryService savedDictionaryService;
    private final SavedWordsService savedWordsService;

    public enum Status { NOT_PRESENT, SAVE_IN_PROGRESS, SAVED, DELETE_IN_PROGRESS }

    LanguageItemVM(ViewContract viewContract, DictionaryModel dictionaryModel, Status status, SavedDictionaryService savedDictionaryService, SavedWordsService savedWordsService) {
        this.viewContract = viewContract;
        this.dictionaryModel = dictionaryModel;
        this.status = status;
        this.savedDictionaryService = savedDictionaryService;
        this.savedWordsService = savedWordsService;
    }

    public String getLanguageName() {
        return dictionaryModel.getLanguageName();
    }

    public String getLogoUrl() {
        return dictionaryModel.getLogoURL();
    }

    public void onClick() {
        if( status == Status.NOT_PRESENT ) {
            saveDictionary();
        }
        else if( status == Status.SAVE_IN_PROGRESS) {
            if(downloadTask != null )
                downloadTask.cancel();
            subscriptions.clear();
            viewContract.showMessage(R.string.dictionary_download_canceled);
            setStatus(Status.NOT_PRESENT);
        }
        else if(status == Status.SAVED) {
            viewContract.onAskUpdateOrRemove(dictionaryModel, new DictionaryListener() {
                @Override
                public void onUpdate(DictionaryModel dictionary) {
                    updateDictionary();
                }

                @Override
                public void onRemove(DictionaryModel dictionary) {
                    removeDictionary();
                }
            });
        }
    }

    private String getDictionaryPath() {
        return Realm.getDefaultInstance().getPath()
                .replace("default.realm", dictionaryModel.getLocale() + ".realm");
    }

    private void saveDictionary() {
        saveDictionary( new ArrayList<>() );
    }
    @AddTrace(name = "saveDictionary")
    private void saveDictionary(List<WordModel> defaultWords) {
        viewContract.onDownloadingDictionary(dictionaryModel);
        setStatus(Status.SAVE_IN_PROGRESS);
        StorageReference storage = FirebaseStorage.getInstance().getReferenceFromUrl(dictionaryModel.getDownloadURL());
        File file = new File(getDictionaryPath());
        Timber.d("Saving file to: %s", getDictionaryPath());

        downloadTask = storage.getFile(file);
        downloadTask.addOnProgressListener(snapshot -> {
                    setStatus(Status.SAVE_IN_PROGRESS);
                    setProgress((int) ((float) (snapshot.getBytesTransferred()) / snapshot.getTotalByteCount() * 100));
                    Timber.d( "Saved %d from %d -> %d percent", snapshot.getBytesTransferred(), snapshot.getTotalByteCount(), getProgress() );
                })
                .addOnCompleteListener(task -> {
                    if( !task.isSuccessful() || !task.isComplete() )
                        return;
                    addSubscription(Completable.mergeArray(
                            savedDictionaryService.saveDictionary(dictionaryModel),
                            savedWordsService.saveWords(dictionaryModel.getLocale(), defaultWords)
                    )
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    () -> setStatus(Status.SAVED),
                                    throwable -> {
                                        setStatus(Status.NOT_PRESENT);
                                        Timber.e(throwable);
                                        viewContract.showError(R.string.dictionary_error_save_failure);
                                    }));
                    Timber.d("Download complete");
                })
                .addOnFailureListener(e -> {
                    Timber.e(e);
                    setStatus(Status.NOT_PRESENT);
                    viewContract.showError(R.string.dictionary_error_save_failure);
                });
    }

    @AddTrace(name = "removeDictionary")
    private void removeDictionary() {
        viewContract.onRemovingDictionary(dictionaryModel);
        setStatus(Status.DELETE_IN_PROGRESS);

        /// delete database file from file system
        Timber.d( "Deleting dictionary at location: %s", getDictionaryPath() );
        File file = new File(getDictionaryPath());
        boolean deleted = file.delete();
        if( !deleted ) {
            setStatus(Status.SAVED);
            viewContract.showError(R.string.dictionary_error_delete_failure);
            return;
        }

        addSubscription(savedDictionaryService.removeDictionary(dictionaryModel)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            setStatus(Status.NOT_PRESENT);
                            Timber.d( "Removed dictionary: %s", dictionaryModel.getLocale() );
                        },
                        throwable -> {
                            setStatus(Status.NOT_PRESENT);
                            viewContract.showError(R.string.dictionary_error_delete_failure);
                        }
                ));
    }


    private void updateDictionary() {
        viewContract.onUpdatingDictionary(dictionaryModel);
        setStatus(Status.SAVE_IN_PROGRESS);

        addSubscription( savedWordsService.getUserDefinedWords(dictionaryModel.getLocale())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        userDefinedWords -> {
                            removeDictionary();
                            saveDictionary(userDefinedWords);
                        },
                        Timber::e
                ));
    }


    @Bindable
    public Status getStatus() {
        return status;
    }
    private void setStatus(Status status) {
        this.status = status;
        notifyPropertyChanged(BR.status);
    }

    @Bindable
    public int getProgress() {
        return progress;
    }
    private void setProgress(int progress) {
        this.progress = progress;
        notifyPropertyChanged(BR.progress);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LanguageItemVM &&
                ((LanguageItemVM) obj).dictionaryModel.equals(dictionaryModel);
    }


    interface DictionaryListener {
        void onUpdate(DictionaryModel dictionary);
        void onRemove(DictionaryModel dictionary);
    }

    interface ViewContract {
        void onDownloadingDictionary(DictionaryModel dictionary);
        void onRemovingDictionary(DictionaryModel dictionary);
        void onUpdatingDictionary(DictionaryModel dictionary);
        void onAskUpdateOrRemove(DictionaryModel dictionary, DictionaryListener listener);
        void showError(@StringRes int message);
        void showMessage(@StringRes int message);
    }
}
