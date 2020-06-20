package ch.so.agi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import ch.ehi.ili2db.base.Ili2db;
import ch.ehi.ili2db.gui.Config;
import ch.ehi.ili2gpkg.GpkgMain;

@Controller
public class MainController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
    private ServletContext servletContext;
   
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<?> uploadFile (
            @RequestParam(name = "doStrokeArcs", required = false) String doStrokeArcs,
            @RequestParam(name = "doNameByTopic", required = false) String doNameByTopic,
            @RequestParam(name = "doDisableValidation", required = false) String doDisableValidation,
            @RequestParam(name = "file", required = true) MultipartFile uploadFile) {
    	
    	try {
            String fileName = uploadFile.getOriginalFilename();
            String strokeArcs = doStrokeArcs;
            String nameByTopic = doNameByTopic;
            String disableValidation = doDisableValidation;
            log.info(disableValidation);

            // we just redirect to the starting page.
            if (uploadFile.getSize() == 0 || fileName.trim().equalsIgnoreCase("") || fileName == null) {
                log.warn("No file was uploaded. Redirecting to starting page.");

                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", servletContext.getContextPath());
                return new ResponseEntity<String>(headers, HttpStatus.FOUND);
            }
            
            File tmpFolder = Files.createTempDirectory("ili2gpkgws-").toFile();
            if (!tmpFolder.exists()) {
                tmpFolder.mkdirs();
            }
            log.info("tmpFolder {}", tmpFolder.getAbsolutePath());

            Path uploadFilePath = Paths.get(tmpFolder.toString(), uploadFile.getOriginalFilename());
            byte[] bytes = uploadFile.getBytes();
            Files.write(uploadFilePath, bytes);
            String uploadFileName = uploadFilePath.toFile().getAbsolutePath();
            log.info("uploadFileName {}", uploadFileName);

            Config settings = createConfig();
            settings.setFunction(Config.FC_IMPORT);
            settings.setDoImplicitSchemaImport(true);
            settings.setDefaultSrsCode("2056"); // TODO Hardcodieren für Naturgefahren.
        	
            if (strokeArcs != null) {
                settings.setStrokeArcs(settings, settings.STROKE_ARCS_ENABLE);
            }
            
            if (nameByTopic != null) {
                settings.setNameOptimization(settings.NAME_OPTIMIZATION_TOPIC);
            }
            
            if (disableValidation != null) {
                settings.setValidation(false);
            }

            if (Ili2db.isItfFilename(uploadFileName)) {
                settings.setItfTransferfile(true);
            }

            String gpkgFileName = uploadFileName.substring(0, uploadFileName.length()-4) + ".gpkg";
            settings.setDbfile(gpkgFileName);

            settings.setDburl("jdbc:sqlite:" + settings.getDbfile());
            settings.setXtffile(uploadFileName);

            Ili2db.run(settings, null);
            
//            File resultFile = new File(resultFileName);
//            InputStream is = new FileInputStream(resultFile);
//            
//            return ResponseEntity
//                    .ok().header("content-disposition", "attachment; filename=" + resultFile.getName())
//                    .contentLength(resultFile.length())
////                  .contentType(MediaType.parseMediaType("text/plain"))
//                    .contentType(MediaType.parseMediaType("application/octet-stream"))
//                    .body(new InputStreamResource(is));                   

            return null;
    	} catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return ResponseEntity.badRequest().contentType(MediaType.parseMediaType("text/plain")).body(e.getMessage());
        }
    }
    
    private Config createConfig() {
        Config settings = new Config();
        new GpkgMain().initConfig(settings);
        return settings;
    }

}
