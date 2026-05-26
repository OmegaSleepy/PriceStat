import matplotlib.pyplot as plt
import pandas as pd

df = pd.read_csv('daily_sunflower_oil_prices_clean.csv')

df["Date"] = pd.to_datetime(df["Date"])
df = df.sort_values('Date').reset_index(drop=True)

df['Rolling_7d'] = df['Price_EUR'].rolling(window=4, center=True, min_periods=1).mean()

df_filtered = df.copy()

fig, ax = plt.subplots(figsize=(12, 6))

ax.plot(df['Date'], df['Price_EUR'], color='lightgray', linestyle='--', label='Raw Daily Averages', alpha=0.7)
ax.scatter(df['Date'], df['Price_EUR'], color='gray', s=15, alpha=0.5)

if not df_filtered.empty:
    ax.plot(df_filtered['Date'], df_filtered['Rolling_7d'], color='navy', lw=2.5, label='Smoothed Trend (7-Day Rolling Avg)')
else:
    print("❌ Error: df_filtered is completely empty. Cannot draw blue line.")

ax.axvline(pd.to_datetime('2026-01-01'), color='green', linestyle=':', lw=2, label='Euro Official Adoption (Jan 1, 2026)')
ax.text(pd.to_datetime('2026-01-02'), 1.13, 'Euro Adoption', color='green', rotation=90, fontsize=10, fontweight='bold')

ax.set_title('Daily Average Price of Sunflower Oil in Bulgaria (EUR)\nNov 2025 – May 2026 (KZP Open Data Analysis)', fontsize=14, fontweight='bold')
ax.set_xlabel('Timeline', fontsize=12)
ax.set_ylabel('Price (€)', fontsize=12)
ax.grid(True, linestyle=':', alpha=0.6)
ax.set_ylim(1.10, 1.46)
ax.legend(loc='upper left', fontsize=10)

plt.setp(ax.get_xticklabels(), rotation=30)

plt.tight_layout()
plt.savefig('daily_sunflower_oil_price_trend.png', dpi=150)
print("Daily plot saved successfully to 'daily_sugar_price_trend.png'.")