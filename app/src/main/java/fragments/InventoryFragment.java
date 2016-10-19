package fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.firstdata.clovergo.MainActivity;
import com.firstdata.clovergo.R;
import com.firstdata.clovergo.client.callback.InventoryCallBack;
import com.firstdata.clovergo.client.internal.model.CloverGoInventoryRequest;
import com.firstdata.clovergo.client.internal.util.AmountUtil;
import com.firstdata.clovergo.client.model.ErrorResponse;
import com.firstdata.clovergo.client.model.Inventory;
import com.firstdata.clovergo.client.model.InventoryResponse;
import com.firstdata.clovergo.client.model.OrderItem;
import com.firstdata.clovergo.client.model.TaxRate;
import com.firstdata.clovergo.client.util.CloverGo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import domain.SampleCloverConstants;

public class InventoryFragment extends Fragment implements InventoryCallBack {

    private Button cardReaderTransaction, manualTransaction, saveOrder;
    private ProgressDialog progressDialog;
    private ListView listView;
    private List<Inventory> inventoryList;
    private ArrayList<OrderItem> mOrderUnsavedItems;
    private InventoryAdaptor inventoryAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inventory, container, false);
        final CloverGo mCloverGo = MainActivity.getCloverGo();
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Loading Inventory....");
        progressDialog.show();

        getActivity().getActionBar().setTitle("Inventory Item");

        final String deviceId = mCloverGo.getDeviceId();
        final String employeeId = mCloverGo.getEmployeeId();
        final String merchantId = mCloverGo.getMerchantId();
        mCloverGo.setInventoryCallBack(InventoryFragment.this);
        CloverGoInventoryRequest inventoryRequest = new CloverGoInventoryRequest();
        inventoryRequest.setDeviceId(deviceId);
        inventoryRequest.setMerchantId(merchantId);
        inventoryRequest.setEmployeeId(employeeId);
        mCloverGo.loadInventory();

        if (mOrderUnsavedItems == null)
            mOrderUnsavedItems = new ArrayList<>();

        cardReaderTransaction = (Button) view.findViewById(R.id.inventoryAddToOrder);
        cardReaderTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double amount = 0;
                double taxAmount = 0;
                HashMap<Double, BigDecimal> taxGroup = new HashMap<>();
                for (OrderItem orderItem : mOrderUnsavedItems) {

                    amount += orderItem.getPrice().multiply(BigDecimal.valueOf(orderItem.getUnitQuantity())).doubleValue();
                    AmountUtil.addTaxRateToHashMap(taxGroup, orderItem);
                }
                taxAmount = AmountUtil.getTotalTaxFromHashMap(taxGroup);
                if (mOrderUnsavedItems.size() > 0) {
                    Fragment fragment = new TransactionFragment();
                    Bundle bundle = new Bundle();
                    bundle.putParcelableArrayList(SampleCloverConstants.BUNDLE_MAPPING.ORDER_ITEMS.name(), mOrderUnsavedItems);
                    fragment.setArguments(bundle);
                    ((MainActivity) getActivity()).replaceFragment(fragment);
                }
            }
        });

        manualTransaction = (Button) view.findViewById(R.id.manualTransactionBtn);
        manualTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double amount = 0;
                double taxAmount = 0;
                HashMap<Double, BigDecimal> taxGroup = new HashMap<>();
                for (OrderItem orderItem : mOrderUnsavedItems) {

                    amount += orderItem.getPrice().multiply(BigDecimal.valueOf(orderItem.getUnitQuantity())).doubleValue();
                    AmountUtil.addTaxRateToHashMap(taxGroup, orderItem);
                }
                taxAmount = AmountUtil.getTotalTaxFromHashMap(taxGroup);
                if (mOrderUnsavedItems.size() > 0) {
                    Fragment fragment = new ManualTransactionFragment();
                    Bundle bundle = new Bundle();
                    bundle.putParcelableArrayList(SampleCloverConstants.BUNDLE_MAPPING.ORDER_ITEMS.name(), mOrderUnsavedItems);
                    fragment.setArguments(bundle);
                    ((MainActivity) getActivity()).replaceFragment(fragment);
                }
            }
        });

        listView = (ListView) view.findViewById(R.id.listViewInventory);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                listView.setItemChecked(position, true);
                Inventory inventory = inventoryList.get(position);

                OrderItem orderItem = addItemToOrder(inventory, 1);
                TextView quantityTxtVw = (TextView) view.findViewById(R.id.inventoryItemQuantity);
                quantityTxtVw.setText(String.format(getResources().getString(R.string.inventory_itemQuantity), orderItem.getUnitQuantity()));
                inventoryAdapter.notifyDataSetChanged();
            }
        });

        /*saveOrder = (Button)view.findViewById(R.id.inventorySaveOrder);
        saveOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOrderUnsavedItems.size() > 0) {
                    mCloverGo.saveOpenOrder(mOrderUnsavedItems);
                    Toast.makeText(getActivity(), "Order Saved", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mCloverGo.setOpenOrderCallBack(new OpenOrderCallBack() {
            @Override
            public void onSuccess(CloverGoOpenOrderResponse cloverGoOpenOrderResponse) {
                Toast.makeText(getActivity(), "Order Saved", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(ErrorResponse errorResponse) {
                Toast.makeText(getActivity(), "Order Unsaved", Toast.LENGTH_SHORT).show();
            }
        });*/

        return view;
    }

    private OrderItem addItemToOrder(Inventory inventoryItem, int quantity) {
        OrderItem orderItem = null;
        double taxes = 0;
        if (orderItem == null) {
            for (OrderItem item : mOrderUnsavedItems) {
                if (item.getInventoryId().equals(inventoryItem.getInventoryId())) {
                    orderItem = item;
                    if (orderItem.getUnitQuantity() == 0)
                        orderItem.setUnitQuantity(1);
                    break;
                }
            }
        }

        if (orderItem == null) {
            String amount = AmountUtil.getCurrencyFormattedAmount(Double.valueOf(inventoryItem.getPrice() / 100d));
            orderItem = new OrderItem();
            orderItem.setName(inventoryItem.getName());
            orderItem.setInventoryId(inventoryItem.getInventoryId());
            orderItem.setPrice(AmountUtil.getDecimalNumberForCurrencyAmount(amount));
            orderItem.setUnitQuantity(quantity);
            if (inventoryItem.getTaxRate() != null) {
                for (TaxRate taxRate : inventoryItem.getTaxRate()) {
                    taxes += taxRate.getRate() / 10000000.00f;
                }
            }
            orderItem.setTaxRateAmount(taxes);
            orderItem.setTaxRateId(null);
            mOrderUnsavedItems.add(orderItem);
        } else if (orderItem.getUnitQuantity() < 999) {
            orderItem.setUnitQuantity(orderItem.getUnitQuantity() + quantity);
        }
        return orderItem;
    }

    private class InventoryAdaptor extends ArrayAdapter<Inventory> implements SectionIndexer {
        HashMap<String, Integer> alphaIndexer;
        String[] sections;

        private InventoryFragment activity;

        private final Context context;
        private final List<Inventory> inventoryList;

        public InventoryAdaptor(Context activity, List<Inventory> inventory) {
            super(activity, R.layout.inventory_item, inventory);
            this.context = activity;
            this.inventoryList = inventory;
            refreshSections(inventoryList);
        }

        @Override
        public Object[] getSections() {
            return sections;
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            return alphaIndexer.get(sections[sectionIndex]);
        }

        @Override
        public int getSectionForPosition(int position) {
            return 0;
        }

        private void refreshSections(List<Inventory> items) {
            alphaIndexer = new HashMap<>();

            for (int i = 0; i < items.size(); i++) {
                String s = items.get(i).getName();
                String ch = s.substring(0, 1);
                ch = ch.toUpperCase();
                if (!alphaIndexer.containsKey(ch))
                    alphaIndexer.put(ch, i);
            }
            ArrayList<String> sectionList = new ArrayList(alphaIndexer.keySet());
            Collections.sort(sectionList);
            sections = new String[sectionList.size()];
            sectionList.toArray(sections);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                LayoutInflater vi;
                vi = LayoutInflater.from(context);
                convertView = vi.inflate(R.layout.inventory_item, null);
            }
            BigDecimal amount = BigDecimal.valueOf(getItem(position).getPrice()).divide(BigDecimal.valueOf(100));
            TextView itemQuantityTextView = (TextView) convertView.findViewById(R.id.inventoryItemQuantity);
            TextView itemNameTextView = (TextView) convertView.findViewById(R.id.inventoryItemName);
            TextView itemPriceTextView = (TextView) convertView.findViewById(R.id.inventoryItemAmount);
            int quantity = 0;

            itemNameTextView.setText(getItem(position).getName());
            itemPriceTextView.setText(AmountUtil.getCurrencyFormattedAmount(amount.doubleValue()));
            itemQuantityTextView.setText(null);

            for (OrderItem item : mOrderUnsavedItems) {
                if (item.getInventoryId().equals(getItem(position).getInventoryId())) {
                    quantity = item.getUnitQuantity();
                    itemQuantityTextView.setText(String.format(getResources().getString(R.string.inventory_itemQuantity), quantity));
                    break;
                }
            }

            return convertView;
        }
    }

    @Override
    public void onSuccess(InventoryResponse inventoryResponse) {
        if (inventoryResponse != null) {
            List<Inventory> inventoryList = inventoryResponse.getInventory();
            this.inventoryList = inventoryList;
            inventoryAdapter = new InventoryAdaptor(getActivity(), inventoryList);
            listView.setAdapter(inventoryAdapter);
        }
        Log.d("TAG", "response" + inventoryResponse);
        progressDialog.dismiss();
    }

    @Override
    public void onFailure(ErrorResponse errorResponse) {
        progressDialog.dismiss();
    }


}
