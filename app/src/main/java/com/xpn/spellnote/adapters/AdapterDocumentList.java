package com.xpn.spellnote.adapters;

import android.content.Context;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.xpn.spellnote.R;
import com.xpn.spellnote.databasehelpers.CreatedDocuments;
import com.xpn.spellnote.databasemodels.DocumentSchema;
import com.xpn.spellnote.fragments.BaseFragmentDocumentList;
import com.xpn.spellnote.util.TagsUtil;
import com.xpn.spellnote.util.Util;

import java.util.ArrayList;

public class AdapterDocumentList extends BaseAdapterDocumentList {

    @Override
    public ItemInteractionListener getArchiveListener() {
        return new ItemInteractionListener() {
            @Override
            public void onClick( int listPosition, View v ) {
                final DocumentSchema document = documentList.get( listPosition );
                CreatedDocuments.moveDocument( document, TagsUtil.CATEGORY_ARCHIVE );
                documentMoveListener.onDocumentMoved();

                Snackbar.make( v, "Archived", Snackbar.LENGTH_LONG ).setAction( "UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CreatedDocuments.moveDocument( document, TagsUtil.CATEGORY_DOCUMENTS );
                        documentMoveListener.onDocumentMoved();
                    }
                } ).show();
            }

            @Override
            public int getDrawableResId() {
                return R.drawable.ic_archive;
            }

            @Override
            public String getExplanation() {
                return "Archive";
            }
        };
    }

    @Override
    public ItemInteractionListener getTrashListener() {
        return new ItemInteractionListener() {
            @Override
            public void onClick(int listPosition, View v) {
                final DocumentSchema document = documentList.get( listPosition );
                CreatedDocuments.moveDocument( document, TagsUtil.CATEGORY_TRASH );
                documentMoveListener.onDocumentMoved();

                Snackbar.make( v, "Moved to trash", Snackbar.LENGTH_LONG ).setAction( "UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CreatedDocuments.moveDocument( document, TagsUtil.CATEGORY_DOCUMENTS );
                        documentMoveListener.onDocumentMoved();
                    }
                } ).show();
            }

            @Override
            public int getDrawableResId() {
                return R.drawable.ic_trash;
            }

            @Override
            public String getExplanation() {
                return "Move to trash";
            }
        };
    }

    @Override
    public ItemInteractionListener getSendListener() {
        return new ItemInteractionListener() {
            @Override
            public void onClick(int listPosition, View v) {
                Util.sendEmail( AdapterDocumentList.this.context, new String[]{}, documentList.get( listPosition ).getTitle(), documentList.get( listPosition ).getContent() );
            }

            @Override
            public int getDrawableResId() {
                return R.drawable.ic_send;
            }

            @Override
            public String getExplanation() {
                return "Send";
            }
        };
    }

    @Override
    public String getDocumentCategory() {
        return TagsUtil.CATEGORY_DOCUMENTS;
    }

    @Override
    public ArrayList<DocumentSchema> getDocumentList() {

        return (ArrayList<DocumentSchema>) CreatedDocuments.getAllDocuments(getDocumentCategory(), fragmentDocumentList.getSortingOrder(), true );
    }


    public AdapterDocumentList(Context context, BaseFragmentDocumentList fragmentDocumentList) {
        super( context, fragmentDocumentList );
    }
}