package com.campusshare.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.campusshare.R;

public class CalculatorActivity extends AppCompatActivity {

    private EditText etOriginalPrice, etAgeMonths;
    private Button btnCalculate;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        etOriginalPrice = findViewById(R.id.et_original_price);
        etAgeMonths = findViewById(R.id.et_age_months);
        btnCalculate = findViewById(R.id.btn_calculate);
        tvResult = findViewById(R.id.tv_result);

        btnCalculate.setOnClickListener(v -> {
            String priceStr = etOriginalPrice.getText().toString();
            String ageStr = etAgeMonths.getText().toString();

            if (priceStr.isEmpty() || ageStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double price = Double.parseDouble(priceStr);
            int age = Integer.parseInt(ageStr);

            // Simple arithmetic operation: Depreciation logic
            // Base credits = 10% of price, reduced by 5% for every year of age
            double baseCredits = price * 0.1;
            double depreciation = (age / 12.0) * 0.05 * baseCredits;
            double finalCredits = Math.max(5, baseCredits - depreciation);

            tvResult.setText(String.format("Estimated Credits: %.0f", finalCredits));
        });
    }
}
