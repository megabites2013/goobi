package de.sub.goobi.helper;

/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information. 
 *     		- http://www.goobi.org
 *     		- http://launchpad.net/goobi-production
 * 		    - http://gdz.sub.uni-goettingen.de
 * 			- http://www.intranda.com
 * 			- http://digiverso.com 
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Linking this library statically or dynamically with other modules is making a combined work based on this library. Thus, the terms and conditions
 * of the GNU General Public License cover the whole combination. As a special exception, the copyright holders of this library give you permission to
 * link this library with independent modules to produce an executable, regardless of the license terms of these independent modules, and to copy and
 * distribute the resulting executable under terms of your choice, provided that you also meet, for each linked independent module, the terms and
 * conditions of the license of that module. An independent module is a module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but you are not obliged to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.apache.log4j.Logger;
import org.goobi.beans.Masterpiece;
import org.goobi.beans.Masterpieceproperty;
import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.beans.Template;
import org.goobi.beans.Templateproperty;
import org.goobi.production.properties.ProcessProperty;
import org.goobi.production.properties.PropertyParser;

import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.helper.exceptions.UghHelperException;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Prefs;

public class VariableReplacer {

    private enum MetadataLevel {
        ALL,
        FIRSTCHILD,
        TOPSTRUCT;
    }

    private static final Logger logger = Logger.getLogger(VariableReplacer.class);

    DigitalDocument dd;
    Prefs prefs;
    UghHelper uhelp;
    // $(meta.abc)
    private final String namespaceMeta = "\\(meta\\.([\\w.-]*)\\)";

    // $(metas.abc)
    private final String namespaceMetaMultiValue = "\\(metas\\.([\\w.-]*)\\)";

    private Process process;
    private Step step;

    @SuppressWarnings("unused")
    private VariableReplacer() {
    }

    public VariableReplacer(DigitalDocument inDigitalDocument, Prefs inPrefs, Process p, Step s) {
        this.dd = inDigitalDocument;
        this.prefs = inPrefs;
        this.uhelp = new UghHelper();
        this.process = p;
        this.step = s;
    }

    /**
     * converts input string into list of arguments.
     * 
     * First the input string gets tokenized, either on double quotation or on white space. Afterwards each parameter gets replaced
     * 
     * @param inString
     * @return
     */

    public List<String> replaceBashScript(String inString) {
        List<String> returnList = new ArrayList<>();
        StrTokenizer tokenizer = new StrTokenizer(inString, ' ', '\"');

        while (tokenizer.hasNext()) {
            String parameter = tokenizer.nextToken();
            parameter = replace(parameter);
            returnList.add(parameter);
        }

        return returnList;
    }

    /**
     * Variablen innerhalb eines Strings ersetzen. Dabei vergleichbar zu Ant die Variablen durchlaufen und aus dem Digital Document holen
     * ================================================================
     */
    public String replace(String inString) {
        if (inString == null) {
            return "";
        }
        inString = inString.replace("${", "(").replace("$(", "(").replace("{", "(").replace("}", ")");
        /*
         * replace metadata, usage: $(meta.firstchild.METADATANAME)
         */
        for (MatchResult r : findRegexMatches(this.namespaceMeta, inString)) {
            if (r.group(1).toLowerCase().startsWith("firstchild.")) {
                inString = inString.replace(r.group(), getMetadataFromDigitalDocument(MetadataLevel.FIRSTCHILD, r.group(1).substring(11), false));
            } else if (r.group(1).toLowerCase().startsWith("topstruct.")) {
                inString = inString.replace(r.group(), getMetadataFromDigitalDocument(MetadataLevel.TOPSTRUCT, r.group(1).substring(10), false));
            } else {
                inString = inString.replace(r.group(), getMetadataFromDigitalDocument(MetadataLevel.ALL, r.group(1), false));
            }
        }

        for (MatchResult r : findRegexMatches(this.namespaceMetaMultiValue, inString)) {
            if (r.group(1).toLowerCase().startsWith("firstchild.")) {
                inString = inString.replace(r.group(), getMetadataFromDigitalDocument(MetadataLevel.FIRSTCHILD, r.group(1).substring(11), true));
            } else if (r.group(1).toLowerCase().startsWith("topstruct.")) {
                inString = inString.replace(r.group(), getMetadataFromDigitalDocument(MetadataLevel.TOPSTRUCT, r.group(1).substring(10), true));
            } else {
                inString = inString.replace(r.group(), getMetadataFromDigitalDocument(MetadataLevel.ALL, r.group(1), true));
            }
        }

        // replace paths and files
        try {
            String processpath = this.process.getProcessDataDirectory().replace("\\", "/");
            String tifpath = this.process.getImagesTifDirectory(false).replace("\\", "/");
            String imagepath = this.process.getImagesDirectory().replace("\\", "/");
            String origpath = this.process.getImagesOrigDirectory(false).replace("\\", "/");
            String metaFile = this.process.getMetadataFilePath().replace("\\", "/");
            String ocrBasisPath = this.process.getOcrDirectory().replace("\\", "/");
            String ocrPlaintextPath = this.process.getOcrTxtDirectory().replace("\\", "/");
            String sourcePath = this.process.getSourceDirectory().replace("\\", "/");
            String importPath = this.process.getImportDirectory().replace("\\", "/");
            String myprefs = ConfigurationHelper.getInstance().getRulesetFolder() + this.process.getRegelsatz().getDatei();

            /* da die Tiffwriter-Scripte einen Pfad ohne endenen Slash haben wollen, wird diese rausgenommen */
            if (tifpath.endsWith(FileSystems.getDefault().getSeparator())) {
                tifpath = tifpath.substring(0, tifpath.length() - FileSystems.getDefault().getSeparator().length()).replace("\\", "/");
            }
            if (imagepath.endsWith(FileSystems.getDefault().getSeparator())) {
                imagepath = imagepath.substring(0, imagepath.length() - FileSystems.getDefault().getSeparator().length()).replace("\\", "/");
            }
            if (origpath.endsWith(FileSystems.getDefault().getSeparator())) {
                origpath = origpath.substring(0, origpath.length() - FileSystems.getDefault().getSeparator().length()).replace("\\", "/");
            }
            if (processpath.endsWith(FileSystems.getDefault().getSeparator())) {
                processpath = processpath.substring(0, processpath.length() - FileSystems.getDefault().getSeparator().length()).replace("\\", "/");
            }
            if (importPath.endsWith(FileSystems.getDefault().getSeparator())) {
                importPath = importPath.substring(0, importPath.length() - FileSystems.getDefault().getSeparator().length()).replace("\\", "/");
            }
            if (sourcePath.endsWith(FileSystems.getDefault().getSeparator())) {
                sourcePath = sourcePath.substring(0, sourcePath.length() - FileSystems.getDefault().getSeparator().length()).replace("\\", "/");
            }
            if (ocrBasisPath.endsWith(FileSystems.getDefault().getSeparator())) {
                ocrBasisPath = ocrBasisPath.substring(0, ocrBasisPath.length() - FileSystems.getDefault().getSeparator().length()).replace("\\", "/");
            }
            if (ocrPlaintextPath.endsWith(FileSystems.getDefault().getSeparator())) {
                ocrPlaintextPath = ocrPlaintextPath.substring(0, ocrPlaintextPath.length() - FileSystems.getDefault().getSeparator().length())
                        .replace("\\", "/");
            }
            if (inString.contains("(tifurl)")) {
                if (SystemUtils.IS_OS_WINDOWS) {
                    inString = inString.replace("(tifurl)", "file:/" + tifpath);
                } else {
                    inString = inString.replace("(tifurl)", "file://" + tifpath);
                }
            }
            if (inString.contains("(origurl)")) {
                if (SystemUtils.IS_OS_WINDOWS) {
                    inString = inString.replace("(origurl)", "file:/" + origpath);
                } else {
                    inString = inString.replace("(origurl)", "file://" + origpath);
                }
            }
            if (inString.contains("(imageurl)")) {
                if (SystemUtils.IS_OS_WINDOWS) {
                    inString = inString.replace("(imageurl)", "file:/" + imagepath);
                } else {
                    inString = inString.replace("(imageurl)", "file://" + imagepath);
                }
            }

            if (inString.contains("(s3_tifpath)")) {
                inString = inString.replace("(s3_tifpath)", S3FileUtils.string2Prefix(tifpath));
            }
            if (inString.contains("(s3_origpath)")) {
                inString = inString.replace("(s3_origpath)", S3FileUtils.string2Prefix(origpath));
            }
            if (inString.contains("(s3_imagepath)")) {
                inString = inString.replace("(s3_imagepath)", S3FileUtils.string2Prefix(imagepath));
            }
            if (inString.contains("(s3_processpath)")) {
                inString = inString.replace("(s3_processpath)", S3FileUtils.string2Prefix(processpath));
            }
            if (inString.contains("(s3_importpath)")) {
                inString = inString.replace("(s3_importpath)", S3FileUtils.string2Prefix(importPath));
            }
            if (inString.contains("(s3_sourcepath)")) {
                inString = inString.replace("(s3_sourcepath)", S3FileUtils.string2Prefix(sourcePath));
            }
            if (inString.contains("(s3_ocrbasispath)")) {
                inString = inString.replace("(s3_ocrbasispath)", S3FileUtils.string2Prefix(ocrBasisPath));
            }
            if (inString.contains("(s3_ocrplaintextpath)")) {
                inString = inString.replace("(s3_ocrplaintextpath)", S3FileUtils.string2Prefix(ocrPlaintextPath));
            }

            if (inString.contains("(tifpath)")) {
                inString = inString.replace("(tifpath)", tifpath);
            }
            if (inString.contains("(origpath)")) {
                inString = inString.replace("(origpath)", origpath);
            }
            if (inString.contains("(imagepath)")) {
                inString = inString.replace("(imagepath)", imagepath);
            }
            if (inString.contains("(processpath)")) {
                inString = inString.replace("(processpath)", processpath);
            }
            if (inString.contains("(importpath)")) {
                inString = inString.replace("(importpath)", importPath);
            }
            if (inString.contains("(sourcepath)")) {
                inString = inString.replace("(sourcepath)", sourcePath);
            }
            if (inString.contains("(ocrbasispath)")) {
                inString = inString.replace("(ocrbasispath)", ocrBasisPath);
            }
            if (inString.contains("(ocrplaintextpath)")) {
                inString = inString.replace("(ocrplaintextpath)", ocrPlaintextPath);
            }
            if (inString.contains("(processtitle)")) {
                inString = inString.replace("(processtitle)", this.process.getTitel());
            }
            if (inString.contains("(processid)")) {
                inString = inString.replace("(processid)", String.valueOf(this.process.getId().intValue()));
            }
            if (inString.contains("(goobiFolder)")) {
                inString = inString.replace("(goobiFolder)", ConfigurationHelper.getInstance().getGoobiFolder());
            }
            if (inString.contains("(scriptsFolder)")) {
                inString = inString.replace("(scriptsFolder)", ConfigurationHelper.getInstance().getScriptsFolder());
            }

            if (inString.contains("(prefs)")) {
                inString = inString.replace("(prefs)", myprefs);
            }
            if (inString.contains("(metaFile)")) {
                inString = inString.replace("(metaFile)", metaFile);
            }

            if (this.step != null) {
                String stepId = String.valueOf(this.step.getId());
                String stepname = this.step.getTitel();

                inString = inString.replace("(stepid)", stepId);
                inString = inString.replace("(stepname)", stepname);
            }

            // replace WerkstueckEigenschaft, usage: (product.PROPERTYTITLE)

            for (MatchResult r : findRegexMatches("\\(product\\.([^)]+)\\)", inString)) {
                String propertyTitle = r.group(1);
                for (Masterpiece ws : this.process.getWerkstueckeList()) {
                    for (Masterpieceproperty we : ws.getEigenschaftenList()) {
                        if (we.getTitel().equalsIgnoreCase(propertyTitle)) {
                            inString = inString.replace(r.group(), we.getWert());
                            break;
                        }
                    }
                }
            }

            // replace Vorlageeigenschaft, usage: (template.PROPERTYTITLE)

            for (MatchResult r : findRegexMatches("\\(template\\.([^)]+)\\)", inString)) {
                String propertyTitle = r.group(1);
                for (Template v : this.process.getVorlagenList()) {
                    for (Templateproperty ve : v.getEigenschaftenList()) {
                        if (ve.getTitel().equalsIgnoreCase(propertyTitle)) {
                            inString = inString.replace(r.group(), ve.getWert());
                            break;
                        }
                    }
                }
            }

            // replace Prozesseigenschaft, usage: (process.PROPERTYTITLE)

            for (MatchResult r : findRegexMatches("\\(process\\.([^)]+)\\)", inString)) {
                String propertyTitle = r.group(1);
                List<ProcessProperty> ppList = PropertyParser.getPropertiesForProcess(this.process);
                for (ProcessProperty pe : ppList) {
                    if (pe.getName().equalsIgnoreCase(propertyTitle)) {
                        inString = inString.replace(r.group(), pe.getValue());
                        break;
                    }
                }

            }

        } catch (SwapException e) {
            logger.error(e);
        } catch (DAOException e) {
            logger.error(e);
        } catch (IOException e) {
            logger.error(e);
        } catch (InterruptedException e) {
            logger.error(e);
        }

        return inString;
    }

    /**
     * Metadatum von FirstChild oder TopStruct ermitteln (vorzugsweise vom FirstChild) und zurückgeben
     * ================================================================
     */

    private String getMetadataFromDigitalDocument(MetadataLevel inLevel, String metadata, boolean multiValue) {
        if (this.dd != null) {
            /* TopStruct und FirstChild ermitteln */
            DocStruct topstruct = this.dd.getLogicalDocStruct();
            DocStruct firstchildstruct = null;
            if (topstruct.getAllChildren() != null && topstruct.getAllChildren().size() > 0) {
                firstchildstruct = topstruct.getAllChildren().get(0);
            }

            /* MetadataType ermitteln und ggf. Fehler melden */
            MetadataType mdt;
            try {
                mdt = this.uhelp.getMetadataType(this.prefs, metadata);
            } catch (UghHelperException e) {
                Helper.setFehlerMeldung(e);
                return "";
            }

            String result = "";
            String resultFirst = null;
            String resultTop = null;
            if (multiValue) {
                resultTop = getAllMetadataValues(topstruct, mdt);
                if (firstchildstruct != null) {
                    resultFirst = getAllMetadataValues(firstchildstruct, mdt);
                }
            } else {
                resultTop = getMetadataValue(topstruct, mdt);
                if (firstchildstruct != null) {
                    resultFirst = getMetadataValue(firstchildstruct, mdt);
                }
            }

            switch (inLevel) {
                case FIRSTCHILD:
                    /* ohne vorhandenes FirstChild, kann dieses nicht zurückgegeben werden */
                    if (resultFirst == null) {
                        logger.info("Can not replace firstChild-variable for METS: " + metadata);
                        result = "";
                    } else {
                        result = resultFirst;
                    }
                    break;

                case TOPSTRUCT:
                    if (resultTop == null) {
                        result = "";
                        logger.warn("Can not replace topStruct-variable for METS: " + metadata);
                    } else {
                        result = resultTop;
                    }
                    break;

                case ALL:
                    if (resultFirst != null && !resultFirst.isEmpty()) {
                        result = resultFirst;
                    } else if (resultTop != null && !resultTop.isEmpty()) {
                        result = resultTop;
                    } else {
                        result = "";
                        logger.warn("Can not replace variable for METS: " + metadata);
                    }
                    break;

            }
            return result;

        } else {
            return "";
        }
    }

    /**
     * Metadatum von übergebenen Docstruct ermitteln, im Fehlerfall wird null zurückgegeben
     * ================================================================
     */
    private String getMetadataValue(DocStruct inDocstruct, MetadataType mdt) {
        List<? extends Metadata> mds = inDocstruct.getAllMetadataByType(mdt);
        if (mds.size() > 0) {
            return ((Metadata) mds.get(0)).getValue();
        } else {
            return null;
        }
    }

    private String getAllMetadataValues(DocStruct ds, MetadataType mdt) {
        String answer = "";
        List<? extends Metadata> metadataList = ds.getAllMetadataByType(mdt);
        if (metadataList != null) {
            for (Metadata md : metadataList) {
                String value = md.getValue();
                if (value != null && !value.isEmpty()) {
                    if (answer.isEmpty()) {
                        answer = value;
                    } else {
                        answer += "," + value;
                    }
                }
            }
        }
        return answer;
    }

    /**
     * Suche nach regulären Ausdrücken in einem String, liefert alle gefundenen Treffer als Liste zurück
     * ================================================================
     */
    public static Iterable<MatchResult> findRegexMatches(String pattern, CharSequence s) {
        List<MatchResult> results = new ArrayList<MatchResult>();
        for (Matcher m = Pattern.compile(pattern).matcher(s); m.find();) {
            results.add(m.toMatchResult());
        }
        return results;
    }
}
