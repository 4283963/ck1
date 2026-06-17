#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
并发压力测试脚本 - 模拟多台冷藏车同时上报数据
用法: python3 concurrent_stress_test.py [并发数] [总请求数]
默认: 20 并发，共 200 次请求
"""

import sys
import time
import json
import random
import threading
import urllib.request
import urllib.error
from collections import Counter, defaultdict
from concurrent.futures import ThreadPoolExecutor, as_completed

BASE_URL = "http://localhost:8080/api"

VEHICLES = [
    {"plateNumber": "京A·88888", "lat_base": 39.9042, "lng_base": 116.4074},
    {"plateNumber": "沪B·66666", "lat_base": 31.2304, "lng_base": 121.4737},
    {"plateNumber": "粤C·12345", "lat_base": 23.1291, "lng_base": 113.2644},
    {"plateNumber": "川D·99999", "lat_base": 30.5728, "lng_base": 104.0668},
    {"plateNumber": "鲁E·77777", "lat_base": 36.6512, "lng_base": 117.1201},
    {"plateNumber": "浙F·55555", "lat_base": 30.2741, "lng_base": 120.1551},
    {"plateNumber": "苏G·33333", "lat_base": 32.0603, "lng_base": 118.7969},
    {"plateNumber": "鄂H·22222", "lat_base": 30.5928, "lng_base": 114.3055},
]

lock = threading.Lock()
results = {
    "success": 0,
    "fail": 0,
    "status_counts": Counter(),
    "errors": [],
    "response_times": []
}


def report_data(vehicle, stuck_temp=False):
    """单次上报请求"""
    plate = vehicle["plateNumber"]
    if stuck_temp:
        temp = 5.00
    else:
        temp = round(random.uniform(2.0, 8.0), 2)
    lat = vehicle["lat_base"] + random.uniform(-0.001, 0.001)
    lng = vehicle["lng_base"] + random.uniform(-0.001, 0.001)

    data = json.dumps({
        "plateNumber": plate,
        "temperature": temp,
        "latitude": round(lat, 6),
        "longitude": round(lng, 6),
        "locationAddress": f"并发测试-{plate}"
    }).encode("utf-8")

    req = urllib.request.Request(
        f"{BASE_URL}/transport/report",
        data=data,
        headers={"Content-Type": "application/json"}
    )
    start = time.time()
    status = 0
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            status = resp.status
            body = json.loads(resp.read().decode("utf-8"))
            ok = body.get("success", False)
            elapsed = (time.time() - start) * 1000
            with lock:
                if ok:
                    results["success"] += 1
                else:
                    results["fail"] += 1
                    results["errors"].append(f"[{plate}] 业务失败: {body.get('message','')}")
                results["status_counts"][status] += 1
                results["response_times"].append(elapsed)
            return ok
    except urllib.error.HTTPError as e:
        status = e.code
        elapsed = (time.time() - start) * 1000
        err_msg = e.read().decode("utf-8", errors="ignore")[:200]
        with lock:
            results["fail"] += 1
            results["status_counts"][status] += 1
            results["errors"].append(f"[{plate}] HTTP {status}: {err_msg}")
            results["response_times"].append(elapsed)
        return False
    except Exception as e:
        elapsed = (time.time() - start) * 1000
        with lock:
            results["fail"] += 1
            results["status_counts"]["Exception"] += 1
            results["errors"].append(f"[{plate}] 异常: {type(e).__name__}: {str(e)[:100]}")
            results["response_times"].append(elapsed)
        return False


def main():
    concurrency = int(sys.argv[1]) if len(sys.argv) > 1 else 20
    total_requests = int(sys.argv[2]) if len(sys.argv) > 2 else 200

    print("=" * 70)
    print("  疫苗冷链运输系统 - 并发压力测试")
    print(f"  目标地址: {BASE_URL}")
    print(f"  并发数: {concurrency}    总请求数: {total_requests}")
    print(f"  测试车辆: {len(VEHICLES)} 辆")
    print("  提示: 所有车辆交替模拟正常温度和固定温度(触发数据失真预警)")
    print("=" * 70)

    # 先测试连接
    print("\n[1/3] 连接测试...")
    try:
        req = urllib.request.Request(f"{BASE_URL}/vehicles")
        with urllib.request.urlopen(req, timeout=5) as resp:
            print(f"    ✓ 后端连接正常，返回状态 {resp.status}")
    except Exception as e:
        print(f"    ✗ 无法连接后端: {e}")
        print("    请先启动后端服务: cd backend && mvn spring-boot:run")
        sys.exit(1)

    # 构造任务
    print(f"\n[2/3] 开始并发测试 ({concurrency} 线程, {total_requests} 次请求)...")
    tasks = []
    for i in range(total_requests):
        v = VEHICLES[i % len(VEHICLES)]
        # 约 40% 请求模拟卡死温度（触发预警检测逻辑）
        stuck = (i % 5 < 2)
        tasks.append((v, stuck))

    start_time = time.time()
    with ThreadPoolExecutor(max_workers=concurrency) as executor:
        futures = {executor.submit(report_data, v, stuck): i for i, (v, stuck) in enumerate(tasks)}
        done = 0
        for f in as_completed(futures):
            done += 1
            if done % 50 == 0 or done == total_requests:
                print(f"    进度: {done}/{total_requests} ({done*100//total_requests}%)")

    total_time = time.time() - start_time

    # 输出结果
    print(f"\n[3/3] 测试结果:")
    print("-" * 50)
    print(f"  总耗时:     {total_time:.2f} 秒")
    print(f"  QPS:        {total_requests / total_time:.2f} 请求/秒")
    print(f"  成功:       {results['success']}")
    print(f"  失败:       {results['fail']}")
    print(f"  成功率:     {results['success']*100/total_requests:.2f}%")
    if results["response_times"]:
        rts = sorted(results["response_times"])
        print(f"  平均响应:   {sum(rts)/len(rts):.1f} ms")
        print(f"  中位响应:   {rts[len(rts)//2]:.1f} ms")
        print(f"  95分位:     {rts[int(len(rts)*0.95)]:.1f} ms")
        print(f"  最慢:       {rts[-1]:.1f} ms")
    print(f"  HTTP状态:   {dict(results['status_counts'])}")

    if results["errors"]:
        print(f"\n  错误摘要（最多显示10条）:")
        seen = set()
        shown = 0
        for err in results["errors"]:
            key = err.split("]: ")[-1] if "]:" in err else err
            if key not in seen:
                seen.add(key)
                print(f"    - {err[:120]}")
                shown += 1
                if shown >= 10:
                    break
        if len(results["errors"]) > shown:
            print(f"    ... 还有 {len(results['errors']) - shown} 条错误未显示")

    print("\n" + "=" * 70)
    if results["fail"] == 0:
        print("  ✓ 所有请求均成功，并发处理正常！")
    elif results["fail"] / total_requests < 0.05:
        print(f"  ⚠ 失败率 {results['fail']*100/total_requests:.2f}% (<5%)，在可接受范围内")
    else:
        print(f"  ✗ 失败率 {results['fail']*100/total_requests:.2f}% 偏高，请检查后端日志")
    print("=" * 70)


if __name__ == "__main__":
    main()
