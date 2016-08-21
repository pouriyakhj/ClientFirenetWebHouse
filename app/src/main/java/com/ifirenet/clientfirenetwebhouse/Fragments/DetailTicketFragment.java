package com.ifirenet.clientfirenetwebhouse.Fragments;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ifirenet.clientfirenetwebhouse.Adapters.DetailTicketRecycleAdapter;
import com.ifirenet.clientfirenetwebhouse.R;
import com.ifirenet.clientfirenetwebhouse.Utils.Client.CreateTicket;
import com.ifirenet.clientfirenetwebhouse.Utils.DetailTicket;
import com.ifirenet.clientfirenetwebhouse.Utils.PublicClass;
import com.ifirenet.clientfirenetwebhouse.Utils.Urls;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DetailTicketFragment.OnDetailTicketListener} interface
 * to handle interaction events.
 */
public class DetailTicketFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    public static final String ARG_NodeId = "NodeId";
    public static final String ARG_UserID = "userId";

    private int nodeId;
    private int userId;
    private int externalStatus = 0;

    private OnDetailTicketListener mListener;
    private View view;
    private ProgressDialog progressDialog;
    private PublicClass publicClass;
    private Spinner spinner;
    private String[] paths = {"اعلام نتیجه بررسی", "بررسی نشده", "مورد تایید", "عدم تایید"};

    public DetailTicketFragment() {
    }

    public static DetailTicketFragment newInstance(String param1, String param2) {
        DetailTicketFragment fragment = new DetailTicketFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NodeId, param1);
        args.putString(ARG_UserID, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            nodeId = getArguments().getInt(ARG_NodeId);
            userId = getArguments().getInt(ARG_UserID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_detail_ticket, container, false);
        init();
        return view;
    }
    private void init(){
        publicClass = new PublicClass(getActivity());



        spinner = (Spinner) view.findViewById(R.id.spinner_detail_ticket_result);
        ArrayAdapter<String>adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item,paths);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        ArrayList<DetailTicket> detailTicketList = getDetailTickets();
    }
    private ArrayList<DetailTicket> getDetailTickets(){
        final ArrayList<DetailTicket> detailTicketList = new ArrayList<>();

        progressDialog = ProgressDialog.show(getActivity(), null,
                "در حال دریافت اطلاعات، لطفا صبر نمایید...", false, false);

        String fullUrl = Urls.baseURL + "ClientPortalService.svc/GetTicketThreads/" + nodeId;
        Ion.with(getActivity())
                .load(fullUrl)
                .asString()
                .withResponse()
                .setCallback(new FutureCallback<Response<String>>() {
                    @Override
                    public void onCompleted(Exception e, Response<String> result) {
                        progressDialog.dismiss();
                        if (e != null){
                            publicClass.showToast("خطا در دریافت اطلاعات!");
                            return;
                        }
                        if (result.getHeaders().code() == 200) {
                            try {
                                JSONArray array = new JSONArray(result.getResult());
                                for (int i = 0; i < array.length(); ++i) {
                                    JSONObject object = array.getJSONObject(i);

                                    Gson gson = new GsonBuilder().create();
                                    DetailTicket detailTicket = gson.fromJson(object.toString(), DetailTicket.class);
                                    detailTicketList.add(detailTicket);
                                }

                                displayData(detailTicketList);
                            } catch (JSONException e1) {
                                progressDialog.dismiss();
                                e1.printStackTrace();
                            }
                        } else publicClass.showToast(result.getHeaders().message());
                    }
                });
        return detailTicketList;
    }
    private void displayData(ArrayList<DetailTicket> detailTicketList){
        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_detail_ticket);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(new DetailTicketRecycleAdapter(getActivity(), detailTicketList));

        final FloatingActionButton  fab = (FloatingActionButton) view.findViewById(R.id.fab_follow_up);
        fab.hide();
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 || dy < 0 && fab.isShown())
                    fab.hide();
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    fab.show();
                }
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //recyclerView.scrollToPosition(10);
                recyclerView.smoothScrollToPosition(0);
            }
        });
    }

    public void showCreateTicketDialog(){
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.layout_popup_create_ticket);
        final EditText input_title = (EditText) dialog.findViewById(R.id.input_create_ticket_alert_dialog_title);
        final EditText input_text = (EditText) dialog.findViewById(R.id.input_create_ticket_alert_dialog_text);
        final EditText input_priority = (EditText) dialog.findViewById(R.id.input_create_ticket_alert_dialog_priority);
        input_priority.setVisibility(View.GONE);
        FrameLayout fl_accept_submit = (FrameLayout) dialog.findViewById(R.id.fl_create_ticket_dialog_accept_submit);
        FrameLayout fl_unAccept_submit = (FrameLayout) dialog.findViewById(R.id.fl__create_ticket_dialog_un_accept_submit);
        fl_accept_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = input_title.getText().toString();
                String text = input_text.getText().toString();
                if (!TextUtils.isEmpty(input_title.getText()) && !TextUtils.isEmpty(input_text.getText()))
                {
                    progressDialog = ProgressDialog.show(getActivity(), null,
                            "در حال دریافت اطلاعات، لطفا صبر نمایید...", false, false);
                    CreateTicket ticket = new CreateTicket(title, text, 1, userId);
                    String fullUrl = Urls.baseURL + "ClientPortalService.svc/CreateThread//" + title + "/" + text + "/" + nodeId + "/" + userId;
                    Ion.with(getActivity())
                            .load(fullUrl)
                            .asString()
                            .withResponse()
                            .setCallback(new FutureCallback<Response<String>>() {
                                @Override
                                public void onCompleted(Exception e, Response<String> result) {
                                    progressDialog.dismiss();
                                    if (e != null){
                                        publicClass.showToast("خطا در دریافت اطلاعات! "+ e.getMessage());
                                        return;
                                    }
                                    if (result.getHeaders().code() == 200) {
                                        try {
                                            JSONObject object = new JSONObject(result.getResult());
                                            if (object.has("text"))
                                                if (object.getBoolean("text")){
                                                    publicClass.showToast("با موفقیت ارسال شد");
                                                    getDetailTickets();
                                                }

                                        } catch (JSONException e1) {
                                            e1.printStackTrace();
                                            publicClass.showToast("خطا در دریافت اطلاعات! "+ e1.getMessage());
                                        }
                                    }
                                }
                            });
                    dialog.dismiss();
                }
            }
        });
        fl_unAccept_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }
    public void showAlertDialog(String title, String text, String yesSubmitText, String noSubmitText){
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.layout_popup_public);
        final TextView txt_title = (TextView) dialog.findViewById(R.id.txt_public_dialog_title);
        final TextView txt_text = (TextView) dialog.findViewById(R.id.txt_public_dialog_text);
        final TextView txt_yes_submit = (TextView) dialog.findViewById(R.id.txt_public_dialog_yes_submit_text);
        final TextView txt_no_submit = (TextView) dialog.findViewById(R.id.txt_public_dialog_no_submit_text);
        FrameLayout fl_accept_submit = (FrameLayout) dialog.findViewById(R.id.fl_public_dialog_accept_submit);
        FrameLayout fl_unAccept_submit = (FrameLayout) dialog.findViewById(R.id.fl_public_dialog_un_accept_submit);

        txt_title.setText(title);
        txt_text.setText(text);
        txt_yes_submit.setText(yesSubmitText);
        txt_no_submit.setText(noSubmitText);

        fl_accept_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                updateTicketExternalStatus();
            }
        });
        fl_unAccept_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void updateTicketExternalStatus(){
        progressDialog = ProgressDialog.show(getActivity(), null,
                "در حال دریافت اطلاعات، لطفا صبر نمایید...", false, false);

        String fullUrl = Urls.baseURL + "ClientPortalService.svc/UpdateTicketExternalStatus/"+ externalStatus + "/" + nodeId + "/" + userId;
        Ion.with(getActivity())
                .load(fullUrl)
                .asString()
                .withResponse()
                .setCallback(new FutureCallback<Response<String>>() {
                    @Override
                    public void onCompleted(Exception e, Response<String> result) {
                        progressDialog.dismiss();
                        if (e != null){
                            publicClass.showToast("خطا در دریافت اطلاعات!");
                            return;
                        }
                        if (result.getHeaders().code() == 200) {
                            publicClass.showToast("نتیجه " + paths[externalStatus + 1] + " با موفقیت ثبت شد.");
                        } else publicClass.showToast(result.getHeaders().message());
                    }
                });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_detail_ticket_fragment, menu);
        super.onCreateOptionsMenu(menu,inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_addTicket:
                showCreateTicketDialog();
                return true;
        }
        return false;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnDetailTicketListener) {
            mListener = (OnDetailTicketListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        if (position != 0){
            externalStatus = position - 1;
            showAlertDialog("اعلام نتیجه بررسی", paths[position] + " را قبول دارید؟", "بله، ارسال شود", "خیر");
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public interface OnDetailTicketListener {
        // TODO: Update argument type and name
        void onDetailTicket();
    }
}