package org.chromium.chrome.browser.vnc.reg;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.util.Map;

public class JsonUtils {

    private static final SerializerFeature[] features = {SerializerFeature.SortField,
            SerializerFeature.WriteMapNullValue,
            SerializerFeature.WriteNullListAsEmpty,
            SerializerFeature.WriteNullStringAsEmpty,
            SerializerFeature.DisableCircularReferenceDetect,
            SerializerFeature.WriteDateUseDateFormat
    };
    
    private static final SerializerFeature[] features2 = {};

	public static String TO_JSON(Object data) {
		return JSON.toJSONString(data, features);
	}
	
	public static String TO_JSON_NF(Object data) {
		return JSON.toJSONString(data, features2);
	}
	
	public static <T> T TO_OBJ(String source, Class<T> clazz){
		return JSON.parseObject(source, clazz);
	}

    public static <T> T TO_OBJ(Object source, Class<T> clazz){
        return TO_OBJ(JSON.toJSONString(source), clazz);
    }

    public static <T> T parse2Generic(String source){
        return JSON.parseObject(source, new TypeReference<T>() {
        });
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parse2Map(String source){
        Map<String, Object> map = null;
        try {
            map = (Map<String, Object>)TO_OBJ(source, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }
}
