package ch.so.agi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
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
import ch.interlis.iom_j.itf.ItfReader;
import ch.interlis.iom_j.xtf.XtfReader;
import ch.interlis.iox.IoxEvent;
import ch.interlis.iox.IoxException;
import ch.interlis.iox.IoxReader;
import ch.interlis.iox_j.EndTransferEvent;
import ch.interlis.iox_j.StartBasketEvent;

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

            String modelName = getModelNameFromTransferFile(uploadFileName);
            settings.setModels(modelName);
            
            // Hardcodiert für altes Naturgefahrenkarten-Modell, damit
            // nicht eine Koordinatenystemoption im GUI exponiert werden
            // muss. Mit LV03 wollen wir nichts mehr am Hut haben.            
            if (modelName.equalsIgnoreCase("Naturgefahrenkarte_SO_V11")) {
                settings.setDefaultSrsCode("21781");
            } else {
                settings.setDefaultSrsCode("2056");
            }

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
            
            File gpkgFile = new File(gpkgFileName);
            InputStream is = new FileInputStream(gpkgFile);
            
            return ResponseEntity
                    .ok().header("content-disposition", "attachment; filename=" + gpkgFile.getName())
                    .contentLength(gpkgFile.length())
//                  .contentType(MediaType.parseMediaType("text/plain"))
                    .contentType(MediaType.parseMediaType("application/octet-stream"))
                    .body(new InputStreamResource(is));                   
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

    private String getModelNameFromTransferFile(String transferFileName) throws IoxException {
        String model = null;
        String ext = getExtensionByString(transferFileName).orElseThrow(IoxException::new);
        
        IoxReader ioxReader = null;

        try {
            File transferFile = new File(transferFileName);

            if (ext.equalsIgnoreCase("itf")) {
                ioxReader = new ItfReader(transferFile);
            } else {
                ioxReader = new XtfReader(transferFile);
            }

            IoxEvent event;
            StartBasketEvent be = null;
            do {
                event = ioxReader.read();
                if (event instanceof StartBasketEvent) {
                    be = (StartBasketEvent) event;
                    break;
                }
            } while (!(event instanceof EndTransferEvent));

            ioxReader.close();
            ioxReader = null;

            if (be == null) {
                throw new IllegalArgumentException("no baskets in transfer-file");
            }

            String namev[] = be.getType().split("\\.");
            model = namev[0];

        } catch (IoxException e) {
            log.error(e.getMessage());
            e.printStackTrace();
            throw new IoxException("could not parse file: " + new File(transferFileName).getName());
        } finally {
            if (ioxReader != null) {
                try {
                    ioxReader.close();
                } catch (IoxException e) {
                    log.error(e.getMessage());
                    e.printStackTrace();
                    throw new IoxException(
                            "could not close interlis transfer file: " + new File(transferFileName).getName());
                }
                ioxReader = null;
            }
        }
        return model;
    } 
    
    private Optional<String> getExtensionByString(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }    
}
