package com.xpn.spellnote.services.dictionary.local;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.xpn.spellnote.models.DictionaryModel;
import com.xpn.spellnote.services.BeanMapper;
import com.xpn.spellnote.services.dictionary.SavedDictionaryService;

import java.util.ArrayList;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import timber.log.Timber;


public class SavedDictionaryServiceImpl implements SavedDictionaryService {

    private final BeanMapper<DictionaryModel, DictionarySchema> dictionaryMapper;
    private final RealmConfiguration realmConfiguration;

    public SavedDictionaryServiceImpl(RealmConfiguration realmConfiguration,
                                      BeanMapper<DictionaryModel, DictionarySchema> dictionaryMapper) {
        this.realmConfiguration = realmConfiguration;
        this.dictionaryMapper = dictionaryMapper;
    }

    @Override
    public Single<ArrayList<DictionaryModel>> getSavedDictionaries() {
        return Single.defer(() -> {
            Realm realm = Realm.getInstance(realmConfiguration);
            realm.refresh();
            RealmResults <DictionarySchema> dictionaries = realm.where(DictionarySchema.class).findAll();

            ArrayList<DictionaryModel> res = Stream.of(dictionaries)
                    .map(dictionaryMapper::mapFrom)
                    .collect(Collectors.toCollection(ArrayList::new));
            realm.close();
            return Single.just(res);
        });
    }

    @Override
    public Single<DictionaryModel> getDictionary(String locale) {
        return Single.defer(() -> {
            Realm realm = Realm.getInstance(realmConfiguration);
            realm.refresh();
            DictionarySchema dictionary = realm.where(DictionarySchema.class)
                    .equalTo("locale", locale)
                    .findFirst();

            DictionaryModel res = dictionaryMapper.mapFrom(dictionary);
            realm.close();
            return Single.just(res);
        });
    }

    @Override
    public Completable saveDictionary(DictionaryModel dictionary) {
        return Completable.fromAction(() -> {
            Timber.d("Save dictionary: %s", dictionary.toString());
            Realm realmInstance = Realm.getInstance(realmConfiguration);
            realmInstance.executeTransaction(realm -> realm.copyToRealmOrUpdate(dictionaryMapper.mapTo(dictionary)));
            realmInstance.close();
        });
    }


    @Override
    public Completable removeDictionary(DictionaryModel dictionary) {
        return Completable.fromAction(() -> {

            Realm realmInstance = Realm.getInstance(realmConfiguration);
            realmInstance.refresh();

            realmInstance.executeTransaction(realm -> realm.where(DictionarySchema.class)
                    .equalTo("locale", dictionary.getLocale())
                    .findFirst()
                    .deleteFromRealm());
            realmInstance.close();
        });
    }
}
