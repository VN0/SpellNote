package com.xpn.spellnote.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.swipe.adapters.BaseSwipeAdapter;
import com.xpn.spellnote.R;
import com.xpn.spellnote.activities.ActivityEditDocument;
import com.xpn.spellnote.databasemodels.DocumentSchema;
import com.xpn.spellnote.util.TagsUtil;

import java.util.ArrayList;

public abstract class BaseAdapterDocumentList extends BaseSwipeAdapter {

    protected Context context;
    ArrayList <DocumentSchema> documentList = new ArrayList<>();

    interface ItemInteractionListener {
        void onClick( int listPosition, View v );
        int getDrawableResId();
        String getExplanation();
    }
    private ItemInteractionListener archive;
    private ItemInteractionListener trash;
    private ItemInteractionListener send;
    public abstract ItemInteractionListener getArchiveListener();
    public abstract ItemInteractionListener getTrashListener();
    public abstract ItemInteractionListener getSendListener();
    public abstract String getDocumentCategory();



    BaseAdapterDocumentList(Context context) {
        this.context = context;
        documentList = getDocumentList();

        archive = getArchiveListener();
        trash = getTrashListener();
        send = getSendListener();
    }

    public abstract ArrayList <DocumentSchema> getDocumentList();

    @Override
    public int getSwipeLayoutResourceId(int position) {
        return R.id.swipe;
    }

    @Override
    public View generateView(int position, ViewGroup viewGroup) {
        return LayoutInflater.from(context).inflate(R.layout.fragment_document_list_item, null);
    }

    @Override
    public void fillValues( final int position, final View convertView ) {

        final RelativeLayout contentPart = (RelativeLayout) convertView.findViewById( R.id.content_part );
        final TextView title = (TextView) convertView.findViewById( R.id.title );
        final TextView text = (TextView) convertView.findViewById( R.id.text );
        final TextView date = (TextView) convertView.findViewById( R.id.date );
        final ImageView archive = (ImageView) convertView.findViewById( R.id.archive );
        final ImageView trash = (ImageView) convertView.findViewById( R.id.trash );
        final ImageView send = (ImageView) convertView.findViewById( R.id.send );

        final DocumentSchema currentItem = documentList.get( position );
        final String titleValue = currentItem.getTitle();
        final String textValue = currentItem.getContent();
        final String dateValue = currentItem.getDateAddedValue();

        title.setText( titleValue.matches("") ? "Untitled" : titleValue );
        text.setText( textValue );
        date.setText( dateValue );

        archive.setImageResource( this.archive.getDrawableResId() );
        trash.setImageResource( this.trash.getDrawableResId() );
        send.setImageResource( this.send.getDrawableResId() );


        archive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BaseAdapterDocumentList.this.closeAllItems();
                BaseAdapterDocumentList.this.archive.onClick( position, v );
            }
        });
        trash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BaseAdapterDocumentList.this.closeAllItems();
                BaseAdapterDocumentList.this.trash.onClick( position, v );
            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BaseAdapterDocumentList.this.send.onClick( position, v );
            }
        });
        contentPart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onContentClick( position, v );
            }
        });



        archive.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText( context, BaseAdapterDocumentList.this.archive.getExplanation(), Toast.LENGTH_SHORT ).show();
                return true;
            }
        });
        trash.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText( context, BaseAdapterDocumentList.this.trash.getExplanation(), Toast.LENGTH_SHORT ).show();
                return true;
            }
        });
        send.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(context, BaseAdapterDocumentList.this.send.getExplanation(), Toast.LENGTH_SHORT).show();
                return true;
            }
        });




        contentPart.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                BaseAdapterDocumentList.this.closeAllItems();
                return false;
            }
        });
    }

    @Override
    public int getCount() {
        return documentList.size();
    }
    @Override
    public Object getItem(int position) {
        return null;
    }
    @Override
    public long getItemId(int position) {
        return documentList.get( position ).getId();
    }




    private void onContentClick(int position, View v) {

        Intent i = new Intent( context, ActivityEditDocument.class );
        i.putExtra( TagsUtil.EXTRA_ID, documentList.get( position ).getId() );
        i.putExtra( TagsUtil.EXTRA_TITLE, documentList.get( position ).getTitle() );
        i.putExtra( TagsUtil.EXTRA_CONTENT, documentList.get( position ).getContent() );
        i.putExtra( TagsUtil.EXTRA_CATEGORY, getDocumentCategory() );
        context.startActivity(i);
    }
}