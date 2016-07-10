package com.dim.bean
/**
 * DexHolder <br/>
 * Created by dim on 2016-07-10.
 */
public class DexHolder {


    public Map<String, Entity> mCLASSList = new HashMap<>();


    public DexHolder(File file) {
        try {
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            Entity entity = null;
            while ((line = reader.readLine()) != null) {
                if (line.matches("classes\\d?.dex")) {
                    entity = new Entity(line);
                    mCLASSList.put(line, entity);
                } else {
                    if (line.length() > 0)
                        entity.addClassItem(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Entity getMainClassDex() {
        return mCLASSList.get("classes.dex");

    }

    public void setMainClass(Dex mainClass) {
        Entity entity = mCLASSList.get("classes.dex");
        if (!entity.equalsClass(mainClass)) {
            entity.hasChange = true;
            for (Object o : mainClass.getAll()) {
                Entity dexEntity = dexEntityOfClassName(o.toString());
                if (dexEntity == null || dexEntity.dexFile.equals("classes.dex")) {
                    //过滤
                } else {
                    dexEntity.hasChange = true;
                    dexEntity.remove(o.toString());
                }
            }
        }
        for (Object o : mainClass.getAll()) {
            getMainClassDex().addClassItem(o.toString());
        }
    }

    public Entity dexEntityOfClassName(String classname) {

        for (Map.Entry<String, Entity> entityEntry : mCLASSList.entrySet()) {

            if (entityEntry.getValue().hasClass(classname)) {
                return entityEntry.getValue();
            }
        }
        return null;
    }

    public class Entity {

        public String dexFile;
        public boolean hasChange;
        public Dex dex;
        public String dexName;
        public Entity(String dexFile) {
            dex = new Dex();
            this.dexFile = dexFile;
            dexName = dexFile.substring(0, dexFile.length() - 4);
        }

        public void addClassItem(String line) {
            dex.addClassItem(line);
        }

        public boolean equalsClass(Dex mainClass) {
            return dex.equals(mainClass);
        }

        public boolean hasClass(String className) {
            return dex.contains(className);
        }


        public void remove(String className) {
            dex.remove(className);
        }

        public void setClass(Dex aClass) {
            dex = aClass;
        }
    }
}
