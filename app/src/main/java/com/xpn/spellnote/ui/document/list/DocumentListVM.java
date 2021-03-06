package com.xpn.spellnote.ui.document.list;

import com.xpn.spellnote.models.DocumentModel;
import com.xpn.spellnote.services.document.DocumentService;
import com.xpn.spellnote.ui.BaseViewModel;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;


public class DocumentListVM extends BaseViewModel {

    private ViewContract viewContract;
    private DocumentService documentService;

    DocumentListVM(ViewContract viewContract, DocumentService documentService) {
        this.viewContract = viewContract;
        this.documentService = documentService;
    }

    void loadDocuments(String category, String sortingOrder, Boolean ascending) {
        addSubscription(documentService
                .getAllDocuments(category, sortingOrder, ascending)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(viewContract::onDocumentsAvailable, Timber::e));
    }

    void restoreDocument(DocumentModel documentModel) {
        addSubscription(documentService
                .saveDocument(documentModel)
                .subscribe());
    }

    public void removeCategory(String category) {
        addSubscription(documentService.removeDocumentCategory(category)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> viewContract.updateDocumentList(),
                        Timber::e
                ));
    }


    interface ViewContract {
        void onDocumentsAvailable(List<DocumentModel> documents);
        void updateDocumentList();
    }
}
