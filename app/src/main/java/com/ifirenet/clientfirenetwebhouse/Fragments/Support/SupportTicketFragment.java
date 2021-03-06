package com.ifirenet.clientfirenetwebhouse.Fragments.Support;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.DrawableCompat;
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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ifirenet.clientfirenetwebhouse.Adapters.TicketRecyclerAdapter;
import com.ifirenet.clientfirenetwebhouse.Fragments.Customer.CustomerTicketFragment;
import com.ifirenet.clientfirenetwebhouse.Links.Tickets;
import com.ifirenet.clientfirenetwebhouse.R;
import com.ifirenet.clientfirenetwebhouse.Utils.Keys;
import com.ifirenet.clientfirenetwebhouse.Utils.PublicClass;
import com.ifirenet.clientfirenetwebhouse.Utils.Support.SupportTicket;
import com.ifirenet.clientfirenetwebhouse.Utils.Support.SupportTicketFilter;
import com.ifirenet.clientfirenetwebhouse.Utils.UserInfo;
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
 * {@link SupportTicketFragment.OnSupportTicketFragmentListener} interface
 * to handle interaction events.
 * Use the {@link SupportTicketFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SupportTicketFragment extends Fragment implements TicketRecyclerAdapter.OnTicketRecyclerAdapterListener {
    View view;
    private RecyclerView recyclerView;
    FloatingActionButton fab;
    ProgressDialog progressDialog;
    ArrayList<Object> allTicketList = new ArrayList<>();
    Object objectFilter;
    PublicClass publicClass;
    private static final String ARG_PARAM2 = "param2";
    private UserInfo userInfo;

    // TODO: Rename and change types of parameters
    private String mParam2;

    private OnSupportTicketFragmentListener mListener;

    public SupportTicketFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static CustomerTicketFragment newInstance() {
        CustomerTicketFragment fragment = new CustomerTicketFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String u = getArguments().getString(Keys.ARG_USER_INFO);
            mParam2 = getArguments().getString(ARG_PARAM2);
            userInfo = new Gson()
                    .fromJson(u, UserInfo.class);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_question_list, container, false);
        objectFilter = null;
        publicClass = new PublicClass(getActivity());
        initRecyclerView();
        return view;
    }

    private void initRecyclerView() {
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_question_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        createItemList();
    }

    private void createItemList() {
        boolean isConnect = publicClass.isConnection();
        if (!isConnect){
            publicClass.showToast("از وصل بودن اینترنت مطمئن شوید");
            return;
        }
        progressDialog = ProgressDialog.show(getActivity(), null,
                "در حال دریافت اطلاعات، لطفا صبر نمایید...", false, false);

        Tickets tickets = new Tickets(userInfo.user.id, -1, -1);
        String fullUrl;
        fullUrl = tickets.getSupportTicketUrl(userInfo.login);

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

                            setSupportTickets(result.getResult());

                        } else publicClass.showToast(result.getHeaders().message());
                    }
                });
    }

    private void setSupportTickets(String json_str){
        allTicketList = new ArrayList<>();
        //String s = result.getResult();
        try {
            JSONArray array = new JSONArray(json_str);
            for (int i = 0; i < array.length(); ++i) {
                JSONObject object = array.getJSONObject(i);

                Gson gson = new Gson();
                allTicketList.add(gson.fromJson(object.toString(), SupportTicket.class));
            }
            displayData(allTicketList);
            progressDialog.dismiss();
        } catch (JSONException e1) {
            progressDialog.dismiss();
            e1.printStackTrace();
        }
    }

    private void displayData(ArrayList<Object> objects){
        recyclerView.setAdapter(new TicketRecyclerAdapter(getActivity().getApplicationContext(), objects, this));

        fab = (FloatingActionButton) view.findViewById(R.id.fab_follow_up);
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

    private void showFilterDialog(final MenuItem item) {
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //dialog.setCancelable(false);
        dialog.setContentView(R.layout.layout_popup_support_ticket_filter);

        final EditText input_trackingCode = (EditText) dialog.findViewById(R.id.input_support_alert_dialog_tracking_code);
        final Spinner sp_result = (Spinner) dialog.findViewById(R.id.spinner_support_alert_dialog_filter_result);
        final Spinner sp_priority = (Spinner) dialog.findViewById(R.id.spinner_support_alert_dialog_filter_priority);
        final Spinner sp_status = (Spinner) dialog.findViewById(R.id.spinner_support_alert_dialog_filter_status);



        if (objectFilter != null){
            if (objectFilter instanceof SupportTicketFilter){
                SupportTicketFilter filter = (SupportTicketFilter) objectFilter;

                input_trackingCode.setText(filter.getTrackingCode());
                sp_priority.setSelection(filter.getPriority());
                sp_result.setSelection(filter.getResult());
                sp_status.setSelection(filter.getStatus());
            }
        }
        FrameLayout fl_accept_submit = (FrameLayout) dialog.findViewById(R.id.fl_support_dialog_accept_submit);
        FrameLayout fl_unAccept_submit = (FrameLayout) dialog.findViewById(R.id.fl_support_dialog_un_accept_submit);

        fl_accept_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int n = sp_priority.getSelectedItemPosition() + sp_result.getSelectedItemPosition() + sp_status.getSelectedItemPosition();
                if (!TextUtils.isEmpty(input_trackingCode.getText()) || n != 0) {
                    ArrayList<Object> objectList = new ArrayList<Object>();
                    for (int i = 0; i < allTicketList.size(); i++) {
                        if (allTicketList.get(i) instanceof SupportTicket) {
                            SupportTicket supportTicket = (SupportTicket) allTicketList.get(i);

                            String code = input_trackingCode.getText().toString();
                            String result = sp_result.getSelectedItem().toString();
                            String priority = sp_priority.getSelectedItem().toString();
                            String status = sp_status.getSelectedItem().toString();
                            if (supportTicket.trackingCode.equals(code) || supportTicket.result.equals(result)
                                    || supportTicket.priority.equals(priority) || supportTicket.status.equals(status)) {
                                objectList.add(supportTicket);
                            }

                                SupportTicketFilter filter = new SupportTicketFilter();

                                filter.setTrackingCode(code);
                                filter.setPriority(sp_priority.getSelectedItemPosition());
                                filter.setResult(sp_result.getSelectedItemPosition());
                                filter.setStatus(sp_status.getSelectedItemPosition());
                                objectFilter = filter;

                        }
                    }
                    if (item != null) {
                        tintMenuIcon(getActivity(), item, R.color.green);
                    }
                    displayData(objectList);
                    // recyclerView.getAdapter().notifyDataSetChanged();
                    dialog.dismiss();
                } else {
                    publicClass.showToast("جهت جستجو اطلاعات خواسته شده را پر نمایید");
                }
            }
        });
        fl_unAccept_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (item != null) {
                    tintMenuIcon(getActivity(), item, android.R.color.white);
                }
                objectFilter = null;
                displayData(allTicketList);
            }
        });

        dialog.show();
    }

    public static void tintMenuIcon(Context context, MenuItem item, @ColorRes int color) {
        Drawable normalDrawable = item.getIcon();
        Drawable wrapDrawable = DrawableCompat.wrap(normalDrawable);
        DrawableCompat.setTint(wrapDrawable, context.getResources().getColor(color));

        item.setIcon(wrapDrawable);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_support_ticket_list_fragment, menu);
        MenuItem menuFilter = menu.findItem(R.id.action_search);
        tintMenuIcon(getActivity(), menuFilter, android.R.color.white);
        super.onCreateOptionsMenu(menu,inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_search:
                showFilterDialog(item);
                return true;
        }
        return false;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnSupportTicketFragmentListener) {
            mListener = (OnSupportTicketFragmentListener) context;
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
    public void onItemClick(Object object) {
            SupportTicket supportTicket = (SupportTicket) object;
            String nodeId = supportTicket.nodeID;
            mListener.onSupportTicket(nodeId);
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnSupportTicketFragmentListener {
        // TODO: Update argument type and name
        void onSupportTicket(String nodeId);
    }
}
