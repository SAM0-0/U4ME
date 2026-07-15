import requests
import json
import uuid
import sys
import urllib3

# Suppress insecure request warnings if any
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

BASE_URL = "https://interface.mahindramobilitysolution.com"

def generate_app_unique_id():
    """Generate a random 16-character alphanumeric string like the Android app."""
    import random
    import string
    chars = string.ascii_lowercase + string.digits
    uid = uuid.uuid4().hex[:15]
    uid += random.choice(chars)
    return uid

def get_base_headers(app_unique_id):
    return {
        "Host": "interface.mahindramobilitysolution.com",
        "Content-Type": "application/json",
        "Connection": "Keep-Alive",
        "User-Agent": "okhttp/4.12.0",
        "Appuniqueid": app_unique_id,
        "Countryid": "41"
    }

def main():
    print("========================================")
    print("      U4Me / Mahindra API Client")
    print("========================================")

    mobile = input("Enter Mobile Number (e.g. +91 9876543210): ").strip()
    password = input("Enter Password: ").strip()

    app_unique_id = generate_app_unique_id()
    session = requests.Session()
    base_headers = get_base_headers(app_unique_id)
    session.headers.update(base_headers)

    print("\n[1/5] Checking Mobile Number Registration...")
    app_login_url = f"{BASE_URL}/V1/auth/one-app/api/appLogin"
    params = {
        "mobileNumber": mobile,
        "appName": "INGLOApp",
        "countryId": "41"
    }
    r = session.get(app_login_url, params=params)
    if r.status_code != 200:
        print(f"[-] Error: Mobile number not registered or network issue. Status: {r.status_code}")
        print(r.text)
        sys.exit(1)
    print("[+] Mobile number verified.")

    print("\n[2/5] Authenticating with Password...")
    login_url = f"{BASE_URL}/V1/auth/login"
    login_headers = {
        "Source": "MMCApp",
        "X-Auth-Username": mobile,
        "X-Auth-Password": password,
        "Applogin": "INGLOApp"
    }
    # We must include the base headers too
    headers = {**base_headers, **login_headers}
    r = session.get(login_url, headers=headers)
    
    if r.status_code != 200:
        print(f"[-] Login failed. Status: {r.status_code}")
        try:
            error_json = r.json()
            msg = error_json.get("message", "")
            if "AE1015" in msg:
                print("[-] Error (AE1015): Invalid mobile number or password.")
            elif "AE1013" in msg:
                print("[-] Error (AE1013): Your account has been locked due to too many failed attempts.")
            elif "AE1016" in msg:
                print("[-] Error (AE1016): Your device ID has been blocked. Try running the script again.")
            else:
                print(f"[-] Error message: {msg}")
        except:
            print(r.text)
        sys.exit(1)
        
    mfa_token = r.headers.get("X-Amzn-Remapped-Authorization")
    if not mfa_token:
        print("[-] Failed to retrieve MFA Token from headers.")
        sys.exit(1)
        
    mfa_token = mfa_token.replace("Bearer ", "")
    print("[+] Authentication successful. MFA token received.")

    # Update session headers with Auth token for next requests
    session.headers.update({"Authorization": f"Bearer {mfa_token}"})

    print("\n[3/5] Requesting OTP...")
    otp_url = f"{BASE_URL}/V1/requestaccess/generateOtpAppIdMapping"
    r = session.get(otp_url)
    otp_resp = r.json()
    if otp_resp.get("status") != "SUCCESS":
        print("[-] Failed to generate OTP.")
        print(r.text)
        sys.exit(1)
    
    print("[+] OTP sent to your registered device/email.")
    otp = input("Enter OTP: ").strip()

    print("\n[4/5] Validating OTP...")
    validate_url = f"{BASE_URL}/V1/auth/validate/otp"
    headers = {**session.headers, "Otp": otp}
    
    # Needs to be a POST request but doesn't require a body based on the android app, just the OTP header
    r = session.post(validate_url, headers=headers)
    jwt_token = r.headers.get("Jwttoken")
    
    if not jwt_token:
        print("[-] Invalid OTP or failed to fetch JWT token.")
        sys.exit(1)
        
    jwt_token = jwt_token.replace("Bearer ", "")
    print("[+] OTP Validated successfully! Logged in.")

    # Replace MFA token with JWT Token for final API calls
    session.headers.update({"Authorization": f"Bearer {jwt_token}"})

    print("\n[5/5] Fetching Vehicle Data...")
    
    # 1. Fetch My Info
    info_url = f"{BASE_URL}/V1/vehicleservice/myInfo"
    r_info = session.get(info_url)
    if r_info.status_code == 200 and r_info.json().get("status") == "Success":
        data = r_info.json().get("data", {})
        print("\n=== USER & VEHICLE INFO ===")
        print(f"Name: {data.get('displayName')}")
        vehicles = data.get("vehicleList", [])
        for v in vehicles:
            print(f"\nVehicle: {v.get('vehicleName')} ({v.get('modelName')} - {v.get('variant')})")
            print(f"VIN: {v.get('vinPlainText')}")
            print(f"Reg No: {v.get('registrationNumber')}")
            print(f"Battery: {v.get('batterySpecification')}")
            print(f"Odometer: {v.get('odometer')} km")
    else:
        print("[-] Failed to fetch profile info.")

    # 2. Fetch Performance Data (SOH)
    perf_url = f"{BASE_URL}/V1/dataanalytics-service/vehicle-performance/fetch-data"
    r_perf = session.get(perf_url)
    if r_perf.status_code == 200:
        perf_data = r_perf.json().get("dataModel", {})
        if perf_data:
            print("\n=== BATTERY HEALTH (SOH) ===")
            print(f"State of Health (SOH): {perf_data.get('hvBattSOH')}%")
            print(f"HV Battery Efficiency: {perf_data.get('hvBattEffy')}%")
            print(f"Total Energy Input:    {perf_data.get('totalEnergyInput')} kWh")
            print(f"Total Energy Consumed: {perf_data.get('totalEnergyConsumption')} kWh")
            print(f"Total Charge Cycles:   {perf_data.get('totalChrgCount')} (AC: {perf_data.get('chargeCountAC')} / DC: {perf_data.get('chargeCountDC')})")
        else:
            print("\n[-] No performance data available.")
    else:
        print("\n[-] Failed to fetch battery performance data.")

    print("\n========================================")
    print("Done!")

if __name__ == "__main__":
    main()
