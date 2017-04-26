/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package playoutCore;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import us.monoid.json.JSONException;
import us.monoid.web.JSONResource;
import us.monoid.web.Resty;

/**
 *
 * @author rombus
 */
public class Test {
    public static void main (String[] args){
        Test t = new Test();
        t.doRun();
    }

    public void doRun(){
        try {
            Resty resty = new Resty();
            JSONResource jsonRes = resty.json("http://localhost:8001/api/medias/1");
            String jobject = (String) jsonRes.toObject().get("name");

            System.out.println(jobject);
        } catch (IOException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JSONException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
