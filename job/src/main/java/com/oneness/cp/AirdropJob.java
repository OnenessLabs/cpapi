package com.oneness.cp;

import org.apache.commons.lang3.StringUtils;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.rdd.RDD;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.util.AccumulatorMetadata;
import org.apache.spark.util.LongAccumulator;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;

import com.oneness.cp.contract.ContractInteraction;
import com.oneness.cp.contract.MyRawTransactionManager;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import scala.Tuple12;
import scala.Tuple2;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public final class AirdropJob {
	
	
	static class SendTxResult implements Serializable{
		String file;
		String msg;
		String user;
		String error;
		String txHash;
		long nonce;
		boolean isMint = false;
		@Override
		public String toString() {
			return String.format("%s|%s|%s|%s|%s",file,user,error,txHash,nonce);
		}
		
		
		
	}
	
	static class ReduceFunc implements Function2<Tuple2,Tuple2,Tuple2> {
	    @Override
	    public Tuple2 call(Tuple2 a, Tuple2 b) {
//	      double[] result = new double[D];
//	      for (int j = 0; j < D; j++) {
//	        result[j] = a[j] + b[j];
//	      }
//	      return result;
	    	return null;
	    }
	  }
	
	static class OkHttpFunc implements Function<SendTxResult,SendTxResult> {

		@Override
		public SendTxResult call(SendTxResult v1) throws Exception {
			OkHttpClient client = new OkHttpClient();

	    	
	    	
	    	MediaType JSON_MEDIA_TYPE =
	                MediaType.parse("application/json; charset=utf-8");
	    	RequestBody requestBody = RequestBody.create(JSON_MEDIA_TYPE, "{}");
	    	
	    	Request request = new Request.Builder()
	                .url("http://www.baidu.com")
	                .post(requestBody)
	                .build();
	    	Response response;
			try {
				response = client.newCall(request).execute();
				System.out.println(response.body().string());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return v1;
		}
		
		
	}
	
	
	
	static class SendTxMapFunc implements Function<SendTxResult,SendTxResult> {
		
		Broadcast<String> airdrop;
		
		public SendTxMapFunc(Broadcast<String> val) {
			airdrop = val;
		}
		
		@Override
		public SendTxResult call(SendTxResult v1) throws Exception {
			String msg = v1.msg;
			
			String [] array = msg.split(" ");
			String toAddress = array[0];
			v1.user = toAddress;
			String cpToken = array[1];
			double cpamount = Double.parseDouble(array[2].toString());
			BigInteger amount = BigDecimal.valueOf(cpamount).multiply(BigDecimal.valueOf(10).pow(18)).toBigInteger();
			String data = ContractInteraction.getData(toAddress,amount);
			long nonce = v1.nonce;
			MyRawTransactionManager txManager = ContractInteraction.getMyRawUserTxManager();
			String hexSignData = txManager.signData(BigInteger.valueOf(nonce), ContractInteraction.GAS_LIMIT, ContractInteraction.airdropAddress, data);
			
			if(StringUtils.isNotBlank(hexSignData)) {
				try {
					if(v1.isMint) {
						EthSendTransaction sendTx = txManager.sendTx(hexSignData);
						if(sendTx.getError()!=null) {
							v1.error = sendTx.getError().getMessage();
						}else {
							String txHash = sendTx.getTransactionHash();
							v1.txHash = txHash;
						}
					}
				}catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			System.out.println(String.format("%s amount:%s signData:%s %s",v1,cpamount,hexSignData,airdrop.value()));
			
			return v1;
		}
		
	}

	/**
	 * 1.file start offset
	 * 2.file num
	 * 3.init nonce
	 * 4.mint switch
	 * 5.airdrop address
	 * @param args
	 * @throws Exception
	 */
  public static void main(String[] args) throws Exception {
    SparkSession spark = SparkSession
      .builder()
      .appName("airdrop")
      .getOrCreate();
    

    System.out.println("args size:"+args.length);
    for(int i=0;i<args.length;i++) {
    	System.out.println(i+"\t"+args[i]);
    }
    long nonce =  Long.parseLong(args[2]);
    
    Boolean isMint = Boolean.valueOf(args[3]);

    
    
    JavaSparkContext jsc = new JavaSparkContext(spark.sparkContext());
//    LongAccumulator accum = spark.sparkContext().longAccumulator();
//    String airdrop = jsc.broadcast("airdrop");
    
    String airdrop = "0x10AB828346c1bBEeaEDc3EdB7be62759B3bb2981";
    if(args.length==5) {
    	airdrop = args[4];
    	
    	System.out.println("airdrop:"+ContractInteraction.airdropAddress);
    	
    }

    Broadcast<String> airAddress =  jsc.broadcast(airdrop);
    
    
    
    int fileoffset = Integer.parseInt(args[0]);
    int fileNum = Integer.parseInt(args[1]);
    int fileEnd = fileoffset + fileNum;
    for(int i=fileoffset;i<=fileEnd;i++) {
    	String file = "s3://0429cp/cp/cp_"+i;
    	JavaRDD<String> lines = spark.read().textFile(file).javaRDD();
    	List<SendTxResult> airdropList = new ArrayList<>();
    	for(String line:lines.collect()) {
        	SendTxResult item = new SendTxResult();
        	item.msg = line;
        	item.file = file;
        	item.nonce = ++nonce;
        	item.isMint = isMint;
        	
        	airdropList.add(item);
        	
        	
        }
    	
    	List<SendTxResult> output = jsc.parallelize(airdropList).map(new SendTxMapFunc(airAddress)).collect();

        
        for(SendTxResult _ret:output) {
        	System.out.println(_ret);
        }
    }
    
    
    spark.stop();
  }
}
