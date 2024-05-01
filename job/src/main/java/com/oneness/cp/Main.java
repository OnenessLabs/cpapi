package com.oneness.cp;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.apache.commons.lang3.StringUtils;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

import com.oneness.cp.contract.ContractInteraction;
import com.oneness.cp.contract.MyRawTransactionManager;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Main {
    public static void main(String[] args) {
    	String airdropAddress = "0x10AB828346c1bBEeaEDc3EdB7be62759B3bb2981";
		String toAddress = "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266";
		String cpToken = "";
		double cpamount = 0.2;
		Credentials credentias =  Credentials.create(ContractInteraction.prikey);
		System.out.println(credentias.getAddress());
		BigInteger amount = BigDecimal.valueOf(cpamount).multiply(BigDecimal.valueOf(10).pow(18)).toBigInteger();
		String data = ContractInteraction.getData(toAddress,amount);
		long nonce = 5937;
		MyRawTransactionManager txManager = ContractInteraction.getMyRawUserTxManager();
		String hexSignData = txManager.signData(BigInteger.valueOf(nonce), ContractInteraction.GAS_LIMIT, airdropAddress, data);
		try {
			EthSendTransaction sendTx = txManager.sendTx(hexSignData);
			String txHash = sendTx.getTransactionHash();
			System.out.println(sendTx.getError()!=null?sendTx.getError().getMessage():"no error");
			System.out.println(txHash);
		}catch (Exception e) {
			e.printStackTrace();
		}
    	

		
    }
}