package com.xpn.spellnote.services.document.local;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.xpn.spellnote.models.DocumentModel;
import com.xpn.spellnote.services.BeanMapper;
import com.xpn.spellnote.services.document.DocumentService;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.Sort;


public class LocalDocumentServiceImpl implements DocumentService {

    private final BeanMapper <DocumentModel, DocumentSchema> mapper;
    private final RealmConfiguration realmConfiguration;

    public LocalDocumentServiceImpl(RealmConfiguration realmConfiguration, BeanMapper<DocumentModel, DocumentSchema> mapper) {
        this.realmConfiguration = realmConfiguration;
        this.mapper = mapper;
    }

    @Override
    public Completable saveDocument(DocumentModel document) {
        return Completable.defer(() -> Completable.fromAction(() -> {
            Realm realmInstance = Realm.getInstance(realmConfiguration);
            realmInstance.executeTransaction(realm -> realm.copyToRealmOrUpdate(mapper.mapTo(document)));
            realmInstance.close();
        }));
    }

    @Override
    public Completable removeDocument(DocumentModel document) {
        return Completable.defer(() -> Completable.fromAction(() -> {
            Realm realmInstance = Realm.getInstance(realmConfiguration);
            DocumentSchema schema = realmInstance.where(DocumentSchema.class).equalTo("id", document.getId()).findFirst();
            if( schema != null )
                realmInstance.executeTransaction(realm -> schema.deleteFromRealm());
            realmInstance.close();
        }));
    }

    @Override
    public Completable removeDocumentCategory(String category) {
        return Completable.defer(() -> Completable.fromAction(() -> {
            Realm realmInstance = Realm.getInstance(realmConfiguration);
            RealmResults<DocumentSchema> documents = realmInstance.where(DocumentSchema.class).equalTo("category", category).findAll();
            realmInstance.executeTransaction(realm -> documents.deleteAllFromRealm());
            realmInstance.close();
        }));
    }

    @Override
    public Completable moveDocument(DocumentModel document, String newCategory) {
        return Completable.defer(() -> Completable.fromAction(() -> {
            DocumentSchema schema = mapper.mapTo(document);
            schema.category = newCategory;

            Realm realmInstance = Realm.getInstance(realmConfiguration);
            realmInstance.executeTransaction(realm -> realm.copyToRealmOrUpdate(schema));
            realmInstance.close();
        }));
    }

    @Override
    public Single<DocumentModel> getDocument(Long id) {
        return Single.defer(() -> {
            Realm realmInstance = Realm.getInstance(realmConfiguration);
            realmInstance.refresh();
            DocumentSchema document = realmInstance.where(DocumentSchema.class).equalTo("id", id).findFirst();

            DocumentModel res = mapper.mapFrom(document);
            realmInstance.close();
            return Single.just(res);
        });
    }

    @Override
    public Single<List<DocumentModel>> getAllDocuments(String category, String orderField, boolean ascending) {

        return Single.defer(() -> {
            Realm realmInstance = Realm.getInstance(realmConfiguration);
            realmInstance.refresh();
            RealmResults<DocumentSchema> documents = realmInstance.where(DocumentSchema.class)
                    .equalTo("category", category)
                    .sort(orderField, ascending ? Sort.ASCENDING : Sort.DESCENDING)
                    .findAll();

            List<DocumentModel> res = Stream.of(documents)
                    .map(mapper::mapFrom)
                    .filter(value -> value.getTitle() != null &&
                            value.getContent() != null &&
                            (!value.getTitle().isEmpty() || !value.getContent().isEmpty()))
                    .collect(Collectors.toCollection(ArrayList::new));

            realmInstance.close();
            return Single.just(res);
        });
    }
}
